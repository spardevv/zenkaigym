package com.zenkaigym.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileValidatorTest {
    private fun valid(h: String, w: String, a: String) =
        ProfileValidator.validate(h, w, a) as ProfileValidation.Valid

    private fun invalid(h: String, w: String, a: String) =
        (ProfileValidator.validate(h, w, a) as ProfileValidation.Invalid).message

    @Test
    fun validInput_isParsed() {
        val v = valid("178", "82,5", "30")
        assertEquals(178, v.heightCm)
        assertEquals(82.5, v.weightKg, 0.0)
        assertEquals(30, v.age)
    }

    @Test
    fun surroundingSpacesAreIgnored() {
        val v = valid(" 170 ", " 70.2 ", " 25 ")
        assertEquals(170, v.heightCm)
        assertEquals(70.2, v.weightKg, 0.0)
    }

    @Test
    fun boundariesAreAccepted() {
        valid("100", "30", "10")
        valid("250", "300", "100")
    }

    @Test
    fun heightOutOfRange_mentionsHeight() {
        assertTrue(invalid("99", "70", "30").contains("Altura"))
        assertTrue(invalid("251", "70", "30").contains("Altura"))
        assertTrue(invalid("", "70", "30").contains("Altura"))
        assertTrue(invalid("17a", "70", "30").contains("Altura"))
    }

    @Test
    fun weightOutOfRange_mentionsWeight() {
        assertTrue(invalid("170", "29,9", "30").contains("Peso"))
        assertTrue(invalid("170", "300.1", "30").contains("Peso"))
        assertTrue(invalid("170", "abc", "30").contains("Peso"))
    }

    @Test
    fun ageOutOfRange_mentionsAge() {
        assertTrue(invalid("170", "70", "9").contains("Idade"))
        assertTrue(invalid("170", "70", "101").contains("Idade"))
        assertTrue(invalid("170", "70", "").contains("Idade"))
    }

    @Test
    fun heightIsReportedBeforeWeight() {
        assertTrue(invalid("0", "0", "0").contains("Altura"))
    }
}

class FoodValidatorTest {
    private fun valid(name: String, k: String, p: String, c: String, f: String) =
        FoodValidator.validate(name, k, p, c, f) as FoodValidation.Valid

    private fun invalid(name: String, k: String, p: String, c: String, f: String) =
        (FoodValidator.validate(name, k, p, c, f) as FoodValidation.Invalid).message

    @Test
    fun validFood_isParsedAndNameTrimmed() {
        val v = valid("  Frango  ", "165", "31", "0", "3,6")
        assertEquals("Frango", v.name)
        assertEquals(165.0, v.kcal, 0.0)
        assertEquals(31.0, v.protein, 0.0)
        assertEquals(3.6, v.fat, 0.0)
    }

    @Test
    fun blankName_isRejected() {
        assertTrue(invalid("   ", "100", "10", "10", "1").contains("nome", ignoreCase = true))
    }

    @Test
    fun missingOrNonNumericValues_areRejected() {
        invalid("Arroz", "", "2", "28", "0")
        invalid("Arroz", "130", "x", "28", "0")
        invalid("Arroz", "130", "2", "", "0")
        invalid("Arroz", "130", "2", "28", "-")
    }

    @Test
    fun negativeValues_areRejected() {
        invalid("Arroz", "-1", "2", "28", "0")
        invalid("Arroz", "130", "-2", "28", "0")
    }

    @Test
    fun macrosCannotExceed100gPer100g() {
        assertTrue(invalid("Estranho", "400", "50", "40", "20").contains("100"))
        valid("Óleo", "884", "0", "0", "100")
    }

    @Test
    fun caloriesCannotExceed900Per100g() {
        assertTrue(invalid("Impossível", "901", "10", "10", "10").contains("900"))
    }
}
