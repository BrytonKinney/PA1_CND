import java.io.File;
import java.io.IOException;

import java.io.IOException;
import java.util.*;

import net.percederberg.mibble.Mib;
import net.percederberg.mibble.MibLoader;
import net.percederberg.mibble.MibLoaderException;
import net.percederberg.mibble.value.ObjectIdentifierValue;
import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityProtocol;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.*;
import java.io.File.*;
import net.percederberg.mibble.*;

public class SNMPClient {

	private String address;
	private Snmp snmp;
	private MibLoader loader;
	private MessageDispatcher mDisp;
	CommandResponder trapResp;
	public SNMPClient(String address)
	{
		super();
		this.address = address;
		try {
			start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Mib loadMibFile(File mibFile)
	{
		loader = new MibLoader();
		try {
			return loader.load(mibFile);
		}
		catch(IOException ex)
		{
			return null;
		}
		catch(MibLoaderException mEx)
		{
			return null;
		}
	}
	public HashMap<String, String> loadMibOIDValuesByName(File mibFile)
	{
		HashMap<String, String> mibMap = new HashMap<>();
		Mib loadedMib = loadMibFile(mibFile);
		for(MibSymbol mSym : loadedMib.getAllSymbols()) {
			ObjectIdentifierValue oidVal;
			if (mSym instanceof MibValueSymbol)
			{
				MibValue mibVal = ((MibValueSymbol)mSym).getValue();
				if(mibVal instanceof ObjectIdentifierValue) {
					oidVal = (ObjectIdentifierValue) mibVal;
					if(oidVal != null)
						mibMap.put(mSym.getName(), oidVal.toDetailString());
				}
			}
		}
		return mibMap;
	}

	public void sendTrap(OID oid, int trapType, String trapVal)
	{
		try
		{

			PDU pdu = new PDU();
			pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new OctetString(new Date().toString())));
			pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, oid));
			Address trapAddr = new UdpAddress("127.0.0.1/162");
			CommunityTarget targ = new CommunityTarget();
			targ.setCommunity(new OctetString("public"));
			targ.setVersion(SnmpConstants.version2c);
			targ.setAddress(trapAddr);
			pdu.add(new VariableBinding(oid, new OctetString(trapVal)));
			pdu.setType(trapType);
			snmp.notify(pdu, targ);
			//snmp.send(pdu, targ);
			snmp.close();
		}
		catch(IOException ex)
		{
			System.out.println(ex.getMessage());
		}
	}
	public void stop() throws IOException
	{
		snmp.close();
	}

	private void start() throws IOException
	{
		TransportMapping transport = new DefaultUdpTransportMapping();
//		mDisp.addMessageProcessingModel(new MPv2c());
	//	SecurityProtocols.getInstance().addDefaultProtocols();
		snmp = new Snmp(transport);
		// Do not forget this line!
		/*
		trapResp = new CommandResponder()
		 */
		/*
		{
			@Override
			public void processPdu(CommandResponderEvent commandResponderEvent)
			{
				PDU pdu = commandResponderEvent.getPDU();
				if(pdu != null)
				{
					System.out.println(pdu.toString());
				}
			}
		};

		snmp.addCommandResponder(trapResp);
		*/
		transport.listen();
	}

	public String getAsString(OID oid) throws IOException
	{
		ResponseEvent event = get(new OID[]{oid});
		return event.getResponse().get(0).getVariable().toString();
	}
/*

	public void getAsString(OID oids,ResponseListener listener)
	{
		try {
			snmp.send(getPDU(new OID[] {oids}), getTarget(),null, listener);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
*/
/*	public List<TreeEvent> walkTree(Target target, OID[] oids)
	{
		TreeUtils tUtil = new TreeUtils(snmp, new DefaultPDUFactory());
		return tUtil.walk(target, oids);
	}
	*/
	public PDU getPDU(OID oids[])
	{
		PDU pdu = new PDU();
		for (OID oid : oids) {
			pdu.add(new VariableBinding(oid));
		}

		pdu.setType(PDU.GET);
		return pdu;
	}

	public ResponseEvent get(OID oids[]) throws IOException
	{
		ResponseEvent event = snmp.send(getPDU(oids), getTarget(), null);
		if(event != null) {
			return event;
		}
		return null;
	}

	public Target getTarget()
	{
		Address targetAddress = GenericAddress.parse(address);
		CommunityTarget target = new CommunityTarget();
		target.setCommunity(new OctetString("public"));
		target.setAddress(targetAddress);
		target.setRetries(2);
		target.setTimeout(1500);
		target.setVersion(SnmpConstants.version2c);
		return target;
	}
	public Map<String, String> walkTable(OID oid, Target target)
	{
		TreeUtils treeUtil = new TreeUtils(snmp, new DefaultPDUFactory());
		List<TreeEvent> subtree = treeUtil.getSubtree(target, oid);
		Map<String, String> mappedResults = new TreeMap<>();
		if(subtree != null || subtree.size() > 0)
		{
			for(TreeEvent ev : subtree)
			{
				if(ev != null)
				{
					VariableBinding[] vars = ev.getVariableBindings();
					if(vars != null || vars.length > 0)
					{
						for(VariableBinding var : vars)
						{
							if(var != null)
							{
								mappedResults.put("." + var.getOid().toString(), var.getVariable().toString());
							}
						}
					}
				}
			}
		}
		return mappedResults;
	}
	public void setOid(OID oid, String newValue)
	{
		PDU pdu = new PDU();
		Variable newVar = new OctetString(newValue);
		VariableBinding newVarBinding = new VariableBinding(oid, newVar);
		pdu.add(newVarBinding);
		pdu.setType(PDU.SET);
		CommunityTarget target = new CommunityTarget();
		target.setCommunity(new OctetString("public"));
		target.setAddress(new UdpAddress("127.0.0.1/161"));
		target.setRetries(2);
		target.setTimeout(1500);
		target.setVersion(SnmpConstants.version2c);
		//pdu.setRequestID(new Integer32(1));
		try
		{
		//	snmp.set(pdu, target);
			ResponseListener respListener = new ResponseListener()
			{
				@Override
				public void onResponse(ResponseEvent responseEvent)
				{
					((Snmp)responseEvent.getSource()).cancel(responseEvent.getRequest(), this);
					System.out.println(responseEvent.getResponse());
					System.out.println(responseEvent.getResponse().getErrorStatusText());
				}
			};
			snmp.send(pdu, target, null, respListener);
			snmp.close();
		}
		catch(IOException ex)
		{
			System.out.println(ex.getMessage());
		}
	}
/*
	public List<List<String>> getTableAsStrings(OID[] oids)
	{
		TableUtils tUtils = new TableUtils(snmp, new DefaultPDUFactory());

		@SuppressWarnings("unchecked")
		List<TableEvent> events = tUtils.getTable(getTarget(), oids, null, null);

		List<List<String>> list = new ArrayList<List<String>>();
		for (TableEvent event : events) {
			if(event.isError()) {
				throw new RuntimeException(event.getErrorMessage());
			}
			List<String> strList = new ArrayList<String>();
			list.add(strList);
			for(VariableBinding vb: event.getColumns()) {
				strList.add(vb.getVariable().toString());
			}
		}
		return list;
	}

	public static String extractSingleString(ResponseEvent event)
	{
		return event.getResponse().get(0).getVariable().toString();
	}
	*/
}