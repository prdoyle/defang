package works.bosk.defang.api;

public record ReflectionEntitlement() implements Entitlement {
    public boolean allows() {
        return true;
    }
}
