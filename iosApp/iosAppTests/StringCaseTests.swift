import XCTest
@testable import JobLog

/// Miroir des cas de `TextCaseTest.kt` (Android) : la logique doit rester identique sur les deux plateformes.
final class StringCaseTests: XCTestCase {

    // MARK: toTitleCase

    func test_toTitleCase_lowercaseWords_capitalizesEachWord() {
        XCTAssertEqual("développeur android senior".toTitleCase(), "Développeur Android Senior")
    }

    func test_toTitleCase_mixedCaseBrandStartingWithLowercase_isKeptAsIs() {
        XCTAssertEqual("iOS engineer".toTitleCase(), "iOS Engineer")
        XCTAssertEqual("iPhone designer".toTitleCase(), "iPhone Designer")
        XCTAssertEqual("eBay".toTitleCase(), "eBay")
    }

    func test_toTitleCase_acronymAlreadyUppercase_isKeptAsIs() {
        XCTAssertEqual("senior SQL analyst".toTitleCase(), "Senior SQL Analyst")
        XCTAssertEqual("QA".toTitleCase(), "QA")
        XCTAssertEqual("UX/UI designer".toTitleCase(), "UX/UI Designer")
    }

    func test_toTitleCase_wordAlreadyCapitalized_isUnchanged() {
        XCTAssertEqual("Lead Developer".toTitleCase(), "Lead Developer")
    }

    func test_toTitleCase_wordWithInnerUppercaseLikeMcDonalds_isKeptAsIs() {
        XCTAssertEqual("McDonald's manager".toTitleCase(), "McDonald's Manager")
    }

    func test_toTitleCase_emptyString_returnsEmptyString() {
        XCTAssertEqual("".toTitleCase(), "")
    }

    func test_toTitleCase_singleLowercaseLetter_isCapitalized() {
        XCTAssertEqual("a".toTitleCase(), "A")
    }

    func test_toTitleCase_multipleSpaces_arePreserved() {
        XCTAssertEqual("iPhone  designer".toTitleCase(), "iPhone  Designer")
        XCTAssertEqual("a  b".toTitleCase(), "A  B")
    }

    func test_toTitleCase_leadingAndTrailingSpaces_arePreserved() {
        XCTAssertEqual(" dev".toTitleCase(), " Dev")
        XCTAssertEqual("dev ".toTitleCase(), "Dev ")
        XCTAssertEqual("  ".toTitleCase(), "  ")
    }

    func test_toTitleCase_hyphenatedWord_capitalizesOnlyTheFirstLetter() {
        XCTAssertEqual("full-stack dev".toTitleCase(), "Full-stack Dev")
    }

    func test_toTitleCase_wordStartingWithDigit_isLeftAlone() {
        XCTAssertEqual("3D artist".toTitleCase(), "3D Artist")
    }

    func test_toTitleCase_accentedFirstLetter_isCapitalizedWithItsAccent() {
        XCTAssertEqual("élève ingénieur".toTitleCase(), "Élève Ingénieur")
    }

    func test_toTitleCase_wordWithDotInside_capitalizesFirstLetterOnly() {
        XCTAssertEqual("node.js developer".toTitleCase(), "Node.js Developer")
    }

    func test_toTitleCase_entirelyLowercaseAcronym_isCapitalizedLikeAnyWord() {
        // Limite connue : sans dictionnaire de sigles, « ios » est indiscernable de n'importe quel mot.
        XCTAssertEqual("ios developer".toTitleCase(), "Ios Developer")
    }

    func test_toTitleCase_emoji_isLeftAlone() {
        XCTAssertEqual("🚀 startup dev".toTitleCase(), "🚀 Startup Dev")
    }

    // MARK: capitalizedFirst

    func test_capitalizedFirst_lowercaseCompany_capitalizesOnlyTheFirstLetter() {
        XCTAssertEqual("acme corp".capitalizedFirst(), "Acme corp")
    }

    func test_capitalizedFirst_alreadyCapitalized_isUnchanged() {
        XCTAssertEqual("Spotify".capitalizedFirst(), "Spotify")
    }

    func test_capitalizedFirst_firstWordIsMixedCaseBrand_isKeptAsIs() {
        XCTAssertEqual("eBay".capitalizedFirst(), "eBay")
        XCTAssertEqual("iRobot corp".capitalizedFirst(), "iRobot corp")
    }

    func test_capitalizedFirst_fullyUppercaseCompany_isUnchanged() {
        XCTAssertEqual("ACME".capitalizedFirst(), "ACME")
    }

    func test_capitalizedFirst_mixedCaseWordNotFirst_doesNotPreventCapitalizingTheFirstWord() {
        XCTAssertEqual("acme eBay partners".capitalizedFirst(), "Acme eBay partners")
    }

    func test_capitalizedFirst_emptyString_returnsEmptyString() {
        XCTAssertEqual("".capitalizedFirst(), "")
    }

    func test_capitalizedFirst_accentedFirstLetter_isCapitalized() {
        XCTAssertEqual("élan".capitalizedFirst(), "Élan")
    }

    func test_capitalizedFirst_startsWithDigit_isUnchanged() {
        XCTAssertEqual("3M".capitalizedFirst(), "3M")
    }

    func test_capitalizedFirst_leadingSpace_isLeftAlone() {
        XCTAssertEqual(" acme".capitalizedFirst(), " acme")
    }
}
