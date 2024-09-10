package works.bosk.defang.api;

/**
 * Entitlements that are simply either granted or not granted,
 * and have no additional structure.
 */
public enum FlagEntitlement implements Entitlement {
    EXIT,
    SET_SYSTEM_FILES,
}
