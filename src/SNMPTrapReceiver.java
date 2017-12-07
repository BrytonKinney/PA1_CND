import org.snmp4j.*;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.security.SecurityProtocol;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.smi.TransportIpAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.AbstractTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.MultiThreadedMessageDispatcher;
import org.snmp4j.util.ThreadPool;

import java.io.IOException;

public class SNMPTrapReceiver implements CommandResponder
{
    private String trapResp;
    public SNMPTrapReceiver()
    {
    }
    public synchronized void startListen()
    {
        try
        {
            TransportIpAddress addr = new UdpAddress("127.0.0.1/162");
            AbstractTransportMapping trans;
            trans = new DefaultUdpTransportMapping((UdpAddress)addr);
            ThreadPool pool = ThreadPool.create("TrapPool", 10);
            MessageDispatcher mDisp = new MultiThreadedMessageDispatcher(pool, new MessageDispatcherImpl());
            mDisp.addMessageProcessingModel(new MPv2c());
            SecurityProtocols.getInstance().addDefaultProtocols();
            Snmp snmp = new Snmp(mDisp, trans);
            snmp.addCommandResponder(this);
            trans.listen();
           /* try
            {
                this.wait();
            }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            */
        }
        catch(IOException ex)
        {
            System.out.println(ex.getMessage());
        }
    }
    public String getTrapResponse()
    {
        return trapResp;
    }
    public synchronized void processPdu(CommandResponderEvent cmdEvent)
    {
        PDU pdu = cmdEvent.getPDU();
        if(pdu != null)
        {
            trapResp = "Type: " + pdu.getType() + "\nVariable Bindings: " + pdu.getVariableBindings();
            System.out.println(trapResp);
        }
    }
}
