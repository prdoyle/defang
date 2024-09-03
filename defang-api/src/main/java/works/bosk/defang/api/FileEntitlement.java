package works.bosk.defang.api;

import java.io.File;

public record FileEntitlement(
        File file, // Needs to be some kind of pattern matcher
        OperationKind operation
) implements Entitlement {
    public boolean allows(File file, OperationKind operation) {
        return this.file.equals(file) && this.operation.equals(operation);
    }
}
