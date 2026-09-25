package `in`.localdukaan
import `in`.localdukaan.core.model.PinRules
import org.junit.Assert.*
import org.junit.Test
class PinRulesTest {
 @Test fun acceptsFourAndSixDigits(){assertTrue(PinRules.valid("1234"));assertTrue(PinRules.valid("123456"))}
 @Test fun rejectsOtherLengthsAndCharacters(){assertFalse(PinRules.valid("12345"));assertFalse(PinRules.valid("12a4"));assertFalse(PinRules.valid(""))}
 @Test fun confirmationMustMatch(){assertTrue(PinRules.confirmationValid("1234","1234"));assertFalse(PinRules.confirmationValid("1234","4321"))}
}
