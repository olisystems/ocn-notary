package shareandcharge.openchargingnetwork.notary

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.web3j.crypto.Credentials

class HelpersTest {

    private val message = "hello, world!"
    private val credentials = Credentials.create("0x0138006a73a6187729ef2a4acfcd39b5d5dfb4f28a37c7911113ae380b51b5d5")
    private val signature = "0x1ac5512e04b65b000c4313ca2850cc32b88ca856245ba05b03e167b35e27abb50f12377e6b3b6919499b3855deff3a3cdbc34140bbe5b014ddd5d20c672c1d2a1b"

    @Test
    fun signsMessage() {
        val actual = signStringMessage(message, credentials.ecKeyPair)
        assertEquals(signature, actual)
    }

    @Test
    fun verifiesMessage() {
        val actual = signerOfMessage(message, signature)
        assertEquals(credentials.address, actual)
    }

}