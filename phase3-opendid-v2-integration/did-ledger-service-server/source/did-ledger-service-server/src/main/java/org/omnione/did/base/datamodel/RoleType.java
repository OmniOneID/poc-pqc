package org.omnione.did.base.datamodel;

/**
 * Description...
 *
 * @author : gwnam
 * @fileName : RoleType
 * @since : 6/14/24
 */
public enum RoleType {
        Tas("TAS"),
        Wallet("월렛"),
        Issuer("발급 사업자"),
        Verifier("검증 사업자"),
        WalletProvider("월렛 사업자"),
        AppProvider("인가앱 사업자"),
        ListProvider("목록 사업자"),
        OpProvider("OP 사업자"),
        KycProvider("KYC 사업자"),
        NotificationProvider("알림 사업자"),
        LogProvider("로그 사업자"),
        PortalProvider("포털 사업자"),
        DelegationProvider("위임 사업자"),
        StorageProvider("저장소 사업자"),
        BackupProvider("백업 사업자"),
        Etc("기타 사업자");

        private String value;

    RoleType(String value) {
        this.value = value;
    }
}
