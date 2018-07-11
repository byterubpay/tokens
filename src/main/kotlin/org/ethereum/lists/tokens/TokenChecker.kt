package org.ethereum.lists.tokens

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.squareup.moshi.JsonEncodingException
import org.ethereum.lists.tokens.model.Token
import org.kethereum.erc55.hasValidEIP55Checksum
import org.kethereum.erc55.withERC55Checksum
import org.kethereum.functions.isValid
import org.kethereum.model.Address
import java.io.File

open class InvalidTokenException(message: String) : IllegalArgumentException(message)
class InvalidChecksum(message: String) : InvalidTokenException("The address is not valid with ERC-55 checksum $message")

class InvalidAddress(address: Address) : InvalidTokenException("The address is not valid $address")
class InvalidDecimals : InvalidTokenException("Decimals must be a number")
class InvalidFileName : InvalidTokenException("Filename must be the address + .json")
class InvalidWebsite : InvalidTokenException("Website invalid")
class InvalidJSON(message: String?) : InvalidTokenException("JSON invalid $message")

fun checkTokenFile(file: File) {
    val jsonObject = Parser().parse(file.reader()) as JsonObject
    val address = Address(jsonObject["address"] as String)
    when {
        !address.isValid() -> throw InvalidAddress(address)

        !address.hasValidEIP55Checksum()
        -> throw InvalidChecksum(address.toString() + " expected: " + address.withERC55Checksum())

        file.name != "${address.hex}.json" -> throw InvalidFileName()
    }
    if (jsonObject["decimals"] !is Int) {
        throw InvalidDecimals()
    }
    if (jsonObject.containsKey("website")) {
        val website = jsonObject["website"] as String
        if (website.isNotEmpty() && !website.matches(websiteRegex)) {
            throw InvalidWebsite()
        }
    }
    try {
        moshi.adapter(Token::class.java).fromJson(file.readText())
    } catch (e: JsonEncodingException) {
        throw InvalidJSON(e.message)
    }

}