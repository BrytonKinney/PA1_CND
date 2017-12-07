import net.percederberg.mibble.value.ObjectIdentifierValue;

public class OIDObj
{
    private ObjectIdentifierValue mOid;

    public OIDObj(ObjectIdentifierValue oid)
    {
        mOid = oid;
    }

    public ObjectIdentifierValue[] getChildren() {
        return mOid.getAllChildren();
    }

    public ObjectIdentifierValue getParent() {
        return mOid.getParent();
    }
}
