package `in`.localdukaan.core.model
object PinRules { fun valid(value:String)=value.all(Char::isDigit) && (value.length==4 || value.length==6); fun confirmationValid(pin:String,confirmation:String)=valid(pin)&&pin==confirmation }
