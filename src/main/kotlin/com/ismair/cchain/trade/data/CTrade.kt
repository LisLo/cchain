package com.ismair.cchain.ui.depot.data

import de.transbase.cchain.extensions.toPrivateKey
import de.transbase.cchain.extensions.toPublicKey

object CTrade {
    val publicKeyPKCS8 = "-----BEGIN PUBLIC KEY-----\n" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqkYkfJok++gnD8LWAbtP\n" +
            "ZdkfQYGrpwQcgbIVz4swlJkN97cP4t1cuffDjBNcETEWTwdlvC+dAcnppRNEHCzf\n" +
            "cwybEK25OlH+9jhL5zvxpZ8KfCUrIlS78bBrRNJEPU7QSYWu6RW0HBSnsWm9EP6V\n" +
            "/cYDYQCA8qW/mEiF3p0CA4Wx4tUI7s+dfkaC3BMgm1KT7T5BAHyAvRQcgVpzac/6\n" +
            "t9+SL05AMnYXnRI/9nvpg+Ed2YOoJ8Lvkb+5mKnA9sSooWgRWtdGKZTS7HmC/M7G\n" +
            "XRj705NPAC1bsVEjH5Y1OHSExwMokP9hI73yPwD6nP9vibTRmOKc/jEhFSFkemxL\n" +
            "bQIDAQAB\n" +
            "-----END PUBLIC KEY-----"

    val privateKeyPKCS8 = "-----BEGIN PRIVATE KEY-----\n" +
            "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQCqRiR8miT76CcP\n" +
            "wtYBu09l2R9BgaunBByBshXPizCUmQ33tw/i3Vy598OME1wRMRZPB2W8L50Byeml\n" +
            "E0QcLN9zDJsQrbk6Uf72OEvnO/Glnwp8JSsiVLvxsGtE0kQ9TtBJha7pFbQcFKex\n" +
            "ab0Q/pX9xgNhAIDypb+YSIXenQIDhbHi1Qjuz51+RoLcEyCbUpPtPkEAfIC9FByB\n" +
            "WnNpz/q335IvTkAydhedEj/2e+mD4R3Zg6gnwu+Rv7mYqcD2xKihaBFa10YplNLs\n" +
            "eYL8zsZdGPvTk08ALVuxUSMfljU4dITHAyiQ/2EjvfI/APqc/2+JtNGY4pz+MSEV\n" +
            "IWR6bEttAgMBAAECggEAAZUrqKbWz3/zFlVAH6hUjsShDZRSz/dx+7iiiu58cfJ8\n" +
            "QUBdMEV7eP+3EcRqTNSbzvnpdawsSu8hXKycVktf0QDIfjQHLpDC4cSt2+/gM/hm\n" +
            "+gwZuwog7G2wiZAPcfxwuNuPv9COVIZ9Sou05gVfx99BJR6ClFQlvu4e+cM+2LaF\n" +
            "BRrx5M4NUX+Eh/a2sza8jT3xoeAbt6LJHqDHFE4tLcblnrxJ5R8kEK0wv5chPSIl\n" +
            "Usn5QqhSUIw/kCzeciVmxyRAA3+n5hnvbsxrEYXNUhVA/eK4ycaVbZUoSrhZF95O\n" +
            "iE5DbzeROWmQiMTWIiTvAx9GV55t2IV+eIdtBxeiQQKBgQDnSkb13KTaiu7OhAXy\n" +
            "X/yG+y3M2Ot4fmn1qbdjieGqoxTzSOmgnvrzPZLq5F6HX8acKWoKzZgKIfvD/dk/\n" +
            "4S36ksG3fs8pllMwHOg/YjalJa47mwFcnz0mkONQk8HyY00037oSuGouq/VK6r32\n" +
            "ffDw5jydN1Go3VPo6HCJ/oX94QKBgQC8dxcTPwQd+5s2NTVQuPJFRIEh7qmB8YxY\n" +
            "0WBdp8pjbp9O/120WI+5xvsOa+N2D+kBeFzVVHW2NrnBvIhhjz51y9GkI3jrt/Uj\n" +
            "BxpADn8e9BOEso/Xsa3u4CpbCaJUiDhxnbAnpQgovaInS0fj1yqV3mS/hv6DPjVn\n" +
            "gzwkKNVHDQKBgQCS5qllVSvbpqyAvHfudqq0rOptFkWQtXHlV1MkRJnxQD15PBEv\n" +
            "NGqdfblHuZ6Uiu9hlihFDkoH/Yej9yI6IXKLmRSy0DcR6emSQHn+cAnXILSmJRBb\n" +
            "XMSBLztBnWds5UdPUt4jL8A5PnzAhZH78gfLaeCL1mvmPhUidCeubdt64QKBgQCq\n" +
            "+CpRxl/4xzDV600TjsQRvISdAMJ7ZGJajI6sR7QdodQZuSYLni+8a9uDCLLPfr11\n" +
            "HPiyeq1SgLDhJSRfxK/38s+a5Kckx7w1a5MPu1btAKTYQ5ikJsbQJkGmVsYRg4YY\n" +
            "4+FTBa6yLt30YaG0+pPiY17oNRGchAm0fuRHNVfN3QKBgQCh+YDWoCD81arDLJWg\n" +
            "JW7NHBxFTLX1e+OWHVFqqKrEYfO8JizyyjhXYmG2ybuC5WBavCCL5KCa1VhwVAfE\n" +
            "T0gYrSy3A5EUm73T10I1IMiLEFZljTVah3Pb6vrs8qP2cKiYcf5TuEqfnIlHpPCN\n" +
            "M6nIelIG88ggBJg1VFo0M6kNuA==\n" +
            "-----END PRIVATE KEY-----"

    val publicKey = publicKeyPKCS8.toPublicKey()
    val privateKey = privateKeyPKCS8.toPrivateKey()
}