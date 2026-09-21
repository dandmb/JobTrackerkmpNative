# JobTracker — contexte projet

> Ce fichier reflète l'état **actuel** du projet (pas son historique). À mettre à jour à chaque
> intervention : ajouter ce qui change, retirer ce qui n'est plus vrai.
> Dernière mise à jour : 2026-09-20 (salaire « max sans min » : validation bloquante avec message, en remplacement du format « jusqu'à Xk »).

## 1. Architecture générale

Application de suivi de candidatures. **Kotlin Multiplatform + MVVM + Room (KMP) + Koin**, avec des
**UI 100 % natives** : Jetpack Compose sur Android, SwiftUI sur iOS. Les deux UI consomment le même
ViewModel partagé.

| Module | Rôle |
|---|---|
| `sharedLogic` | Logique partagée (KMP : `androidMain`, `commonMain`, `iosMain`). Domaine, use cases, repository, Room, Koin, `JobOfferListViewModel`. Compilé en framework iOS statique `SharedLogic` (`iosArm64` + `iosSimulatorArm64`). |
| `androidApp` | App Android : Compose + Material3, thème, écrans (`ui/joboffer`, `ui/theme`, `ui/util`). |
| `iosApp` | App iOS : SwiftUI (`iosApp/iosApp/Core`, `Features/JobOffer`, `Resources/Fonts`) + tests XCTest (`iosApp/iosAppTests`). Xcode lance `./gradlew :sharedLogic:embedAndSignAppleFrameworkForXcode` avant de compiler. Groupes Xcode « synchronisés » : tout fichier ajouté dans `iosApp/iosApp/` est inclus automatiquement dans la cible. |
| `sharedUI` | Module Compose Multiplatform **quasi vide** (`App.kt` du template). Non utilisé par les écrans réels : l'UI Android vit dans `androidApp`. |

Couches dans `sharedLogic` (`com.dmb.jobtracker`) : `domain` (model, repository, usecase) →
`data` (Room : entity/dao/converter/migrations, mapper, `JobOfferRepositoryImpl`) → `presentation`
(`JobOfferListViewModel`, `JobOfferListState`) → `di` (modules Koin).
`JobOfferListViewModel` est du Kotlin pur (pas d'`androidx.lifecycle.ViewModel`) avec son propre
`CoroutineScope` ; il expose `state: StateFlow<JobOfferListState>` (`@NativeCoroutinesState`).
Côté iOS : `KoinInitKt.doInitKoin()` dans `iOSApp.init`, puis `KoinHelper().jobOfferListViewModel()`.

## 2. Stack technique (source : `gradle/libs.versions.toml`)

| Élément | Version |
|---|---|
| Kotlin | 2.4.20 |
| Android Gradle Plugin | 9.1.1 |
| KSP | 2.3.12 |
| Compose Multiplatform | 1.12.0 |
| Material3 (`org.jetbrains.compose.material3`) | 1.12.0-alpha03 |
| Room (runtime/compiler/plugin) | 2.8.5 |
| AndroidX SQLite (bundled) | 2.7.1 |
| Koin (core / android / compose) | 4.2.2 |
| KMP-NativeCoroutines | 1.0.6 (Android : plugin Gradle ; iOS : package SPM `KMPNativeCoroutines`, branche `master`, + RxSwift 6.10.2 résolu) |
| kotlinx-coroutines (core et test, alignés) | 1.10.1 |
| Turbine (test de Flow) | 1.2.1 |
| Kover (couverture) | 0.9.9 |
| JUnit 4 (tests app Android) | 4.13.2 |
| kotlinx-datetime | 0.6.1 |
| androidx.lifecycle | 2.11.0 |
| androidx.activity | 1.13.0 |
| Material Icons Extended | 1.7.8 |
| compileSdk / targetSdk / minSdk | 37 / 37 / 24 |
| iOS deployment target | 18.2 |
| JVM target | 11 |

## 3. Modèle de données

`domain/model/JobOffer.kt` :

```
JobOffer(
  id: Long = 0, title: String, company: String,
  url: String?, location: String?, source: String?, salaryRange: String?,
  appliedDate: LocalDate, interviewDate: LocalDate?, resultDate: LocalDate?,
  status: ApplicationStatus, notes: String?
)
enum ApplicationStatus { PENDING, APPLIED, INTERVIEW, REJECTED, ACCEPTED }
```

Room : `JobOfferEntity` (table `job_offers`, dates stockées en `*EpochDays: Long`, + `createdAtEpochMillis`).
`AppDatabase` version **2** (`MIGRATION_1_2` ajoute location, source, salaryRange, interviewDate, resultDate),
fichier `job_offers.db`, schémas exportés dans `sharedLogic/schemas`.
Validation dans `AddJobOfferUseCase` : titre et entreprise non vides (`require`).

## 4. Design system

Référence = **Android** (`androidApp/.../ui/theme/`). iOS reproduit les mêmes valeurs.

### Palette (hex)

| Rôle | Clair | Sombre |
|---|---|---|
| primary (Teal) | `#0D6E68` | `#6FD4C8` |
| onPrimary | `#FFFFFF` | `#00201B` |
| primaryContainer | `#B0F1E4` | `#00504A` |
| onPrimaryContainer | `#00201B` | `#B0F1E4` |
| secondary (Corail) | `#E8734A` | `#FFB59D` |
| onSecondary | `#FFFFFF` | `#5B1900` |
| secondaryContainer | `#FFDBCB` | `#7D2C0C` |
| onSecondaryContainer | `#3A0A00` | `#FFDBCB` |
| surface / onSurface (surchargés) | `#F7FBF9` / `#161D1C` | `#0F1514` / `#DDE4E1` |
| error | `#BA1A1A` | `#FFB4AB` |

Valeurs **Material3 par défaut** (non surchargées dans `Theme.kt`, mais réellement affichées sur Android ;
lues dans les tokens `ColorLightTokens`/`ColorDarkTokens` de material3 1.12.0-alpha03) :

| Rôle | Clair | Sombre | Utilisé pour |
|---|---|---|---|
| background | `#FEF7FF` | `#141218` | fond de l'écran (Scaffold) |
| onBackground | `#1D1B20` | `#E6E0E9` | texte hors carte |
| surfaceContainerHighest | `#E6E0E9` | `#36343B` | fond des `Card` |
| onSurfaceVariant | `#49454F` | `#CAC4D0` | texte secondaire |
| outlineVariant | `#CAC4D0` | `#49454F` | divider, contour du chip |

Statuts — **adaptatifs clair/sombre**, identiques sur les deux plateformes. Utilisés comme couleur de **texte** (chip de la carte, badges de la carte stats) ;
chaque valeur atteint **WCAG AA (≥ 4.5:1)** sur la carte (`surfaceContainerHighest`) ET sur un badge de la carte stats (couleur teintée sur `primaryContainer`) :

| Statut | Clair | Sombre |
|---|---|---|
| PENDING | `#57535A` | `#CAC4D0` |
| APPLIED | `#0B5E58` | `#78DDD1` |
| INTERVIEW | `#913312` | `#FFB59D` |
| REJECTED | `#9F1616` | `#FFB4AB` |
| ACCEPTED | `#235F26` | `#9BD99F` |

Opacité de la teinte de fond des badges de statut : **0,16 en clair, 0,08 en sombre** (une teinte plus forte éclaircit le fond du badge et fait passer le texte sous 4.5:1).
Les anciennes valeurs de marque (Coral40 `#E8734A`, Teal40 `#0D6E68`… utilisées comme texte) échouaient AA même en clair (ex. corail 2.3:1) : ne pas les réutiliser pour du texte.
**Règle pour toute nouvelle couleur de texte/statut** : vérifier ≥ 4.5:1 (≥ 3:1 seulement pour texte ≥ 24 px ou icône/contour informatif) sur chaque fond où elle s'affiche, dans les deux modes.

Où c'est défini : Android → `ui/theme/Color.kt` (constantes `Status*Light/Dark`, `StatusBadgeTint*`), `StatusColors.kt` (`StatusPalette` + `LocalStatusPalette`, lue par `ApplicationStatus.color()`),
fourni par `JobTrackerTheme` dans `Theme.kt` ; iOS → `iosApp/Core/Theme/Color+Theme.swift` (`Color(light:dark:)`, `statusBadgeTintAlpha(for:)`),
mapping statut→couleur/libellé dans `Core/Extensions/JobOffer+Display.swift`.

### Typographie

Police : **Plus Jakarta Sans**, poids Regular/Medium/SemiBold/Bold.

| Style | Poids | Taille / interligne |
|---|---|---|
| headlineSmall | Bold | 24 / 30 |
| titleLarge | SemiBold | 20 / 26 |
| titleMedium | SemiBold | 16 / 22 |
| bodyLarge | Regular | 16 / 22 |
| bodyMedium | Regular | 14 / 20 |
| labelLarge | Medium | 13 / 18 |

- Android : `ui/theme/Type.kt` (Google Fonts téléchargeable via `font_certs.xml`, `AppTypography`).
- iOS : `iosApp/Core/Theme/AppTypography.swift` — `AppTextStyle` (mêmes valeurs) + modificateur
  `.appTextStyle(_:)`. Les 4 `.ttf` statiques (instances générées depuis la police variable Google Fonts, licence OFL)
  sont dans `iosApp/iosApp/Resources/Fonts/` ; ils sont enregistrés au runtime via CoreText (pas de `UIAppFonts`).
  Suit Dynamic Type (`@ScaledMetric`).

### Composants de liste (mêmes valeurs sur les deux plateformes)

- Carte candidature : fond surfaceContainerHighest, coin 12, padding 16 ; titre en title case (titleMedium, letterSpacing 0.15),
  entreprise en sentence case (bodyMedium, onSurfaceVariant) ; crayon d'édition en haut à droite ; localisation (icône 14 + labelLarge) si renseignée ;
  chip de statut cliquable (hauteur visuelle 32, coin 8, contour outlineVariant 1, label coloré par statut, flèche « dropdown ») ; divider ; dates
  Postulé / Entretien / Résultat (label MAJUSCULES 11 medium, tracking 0.5, onSurfaceVariant ; valeur labelLarge SemiBold, format `j mmm` **en français** : « 5 sept. », « 12 févr. ») ; salaire `💰 …`.
  Zones tactiles : crayon ≥ 48, chip ≥ 44 pt sur iOS (32 visuels ; Android : 48 dp automatiques via M3).
- Carte stats : fond primaryContainer, coin 12, padding 20 ; total 34 Bold (interligne 30) ; « candidature(s) suivie(s) » à 80 % ; badges par statut (coin 10, padding 10×6, fond teinté : voir opacité ci-dessus)
  **qui passent à la ligne** (Android `FlowRow`, iOS `Core/Layout/FlowLayout.swift`).
- Marges de la carte stats : 16 sur les côtés et 12 avec l'élément suivant, sur les deux plateformes (Android : `Column.padding(16.dp)` + `spacedBy(12.dp)` ; iOS : `listRowInsets(top: 8, leading: 16, bottom: 8, trailing: 16)` sur la `Section`, plus les 4 de marge haute des cartes suivantes).
- Écran de liste : marge basse de 88 (dp/pt) pour que le FAB ne masque pas la dernière carte ; FAB = icône « + » (label d'accessibilité « Ajouter une candidature »).
- Accessibilité : libellé + valeur d'une date lus d'un bloc ; total + « candidatures suivies » lus d'un bloc ; chip « Statut : X » ; crayon « Modifier la candidature <titre> » ;
  icônes iOS qui suivent Dynamic Type (`@ScaledMetric`), chip iOS en `minHeight` (grandit avec la police).
- Logique de casse : Android `ui/util/TextCase.kt` ↔ iOS `Core/Extensions/String+Case.swift`.
  `toTitleCase` : première lettre de chaque mot en majuscule **sauf** si le mot porte déjà une majuscule après sa 1re lettre (sigles `SQL`, `QA`, marques en casse mixte `iOS`, `iPhone`, `eBay`), laissé tel quel ;
  `capitalizedFirst` applique la même exception au premier mot. Limite connue : « ios developer » saisi tout en minuscules devient « Ios Developer » (aucun dictionnaire de sigles).

## 5. État des lieux fonctionnel

Fait :
- CRUD des candidatures (ajout, édition via feuille de formulaire, suppression, changement de statut depuis la carte) — Android et iOS.
- Recherche (titre/entreprise, insensible à la casse, faite côté UI, les stats portent sur **toutes** les offres).
- Tri : plus récent / plus ancien / A→Z / Z→A.
- Carte de statistiques (total + compteurs par statut).
- Suppression : swipe. Android : `SwipeToDismissBox` + snackbar **avec « Annuler »** (ré-ajoute l'offre via `onAddOffer`, même id ; la carte revient à l'état normal et la liste défile jusqu'à elle). iOS : `swipeActions` + **`confirmationDialog`** avant suppression.
- Persistance Room avec migration 1→2.

Constats / reste à faire (observés dans le code, pas de roadmap officielle) :
- `JobOfferListEvent` est une classe vide ; `sharedUI` est un template non utilisé.
- **Corrigé et validé explicitement** : toute action du ViewModel (`onAddOffer`, `onUpdateOffer`, `onStatusChanged`, `onDeleteOffer`) passe par `launchReportingErrors` : une exception du repository est reportée dans `state.errorMessage` au lieu de s'échapper du scope (crash) ; `CancellationException` est relancée (annuler le scope via `onCleared` n'est pas une erreur). **Le `catch (Exception)` élargi aux 4 actions a été validé par le propriétaire du projet** (pas seulement appliqué par défaut). Justification : (1) une vraie panne SQLite / disque plein n'est pas une `IllegalArgumentException`, donc attraper ce seul type laisserait le plantage en production ; (2) `onAddOffer`/`onUpdateOffer` avaient exactement le même risque. Quand l'exception n'a pas de message (`null` ou blanc), `errorMessage` vaut **« Une erreur est survenue, réessaie. »** (`DEFAULT_ERROR_MESSAGE`) : l'utilisateur voit toujours qu'il s'est passé quelque chose. Même règle pour une erreur du flux de chargement.
- **Corrigé** : la recherche « espaces seulement » ne filtre rien sur les deux plateformes (Android `isBlank()`, iOS `trimmingCharacters(in: .whitespacesAndNewlines).isEmpty`).
- **Formulaires** : la logique est désormais UNIQUE, dans `sharedLogic` (`presentation/form/JobOfferFormLogic`), voir §6. Il ne reste qu'une limite connue : la lecture d'un salaire décimal hérité (« 45.5k ») donne 455 (le formulaire n'écrit que des entiers) ; et min > max n'est pas contrôlé.
- `AddJobOfferSheet.kt` (Android, code mort jamais référencé) a été **supprimé**.
- **Constat** : `errorMessage` du ViewModel n'est remis à `null` que par la prochaine émission du flux de données (pas de « dismiss » explicite).
- **Constat** : tri alphabétique par point de code Unicode, non localisé (« École » passe après « Zoé »), identique sur les deux plateformes.
- Les fichiers `data/local/*` (amorçage Room) apparaissent à 0 % dans Kover **alors qu'ils sont testés** : les tests Room tournent en Kotlin/Native (`iosTest`), que Kover ne mesure pas (il ne mesure que JVM/Android).
- Le libellé salaire garde l'emoji `💰` (rendu différent Apple/Noto, annoncé « sac d'argent » par les lecteurs d'écran) : à remplacer par une icône + libellé « Salaire » si souhaité.

## 5 bis. Tests et couverture

### Commandes (racine du projet)

| Objet | Commande |
|---|---|
| **Toute la suite Kotlin** (Android JVM + natif iOS + app Android) | `./gradlew allUnitTests` |
| Tests Android uniquement | `./gradlew testAndroidHostTest :androidApp:testDebugUnitTest` |
| Tests natifs iOS du module partagé (simulateur) — inclut les tests Room réels (DAO, migration) | `./gradlew :sharedLogic:iosSimulatorArm64Test` |
| **Tests Swift (XCTest)** | `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme iosAppTests -destination 'platform=iOS Simulator,name=<simulateur>' -enableCodeCoverage YES` |
| **Rapport de couverture Kotlin (HTML)** | `./gradlew koverHtmlReport` → `build/reports/kover/html/index.html` (XML : `./gradlew koverXmlReport` → `build/reports/kover/report.xml`) |
| Couverture brute (Composables inclus) | `./gradlew koverHtmlReport -PkoverFull` |
| Couverture Swift (après `xcodebuild test -resultBundlePath X.xcresult`) | `xcrun xccov view --report X.xcresult` |

**Piège** : `./gradlew test` ne lance **pas** les tests de `sharedLogic` (un module Kotlin Multiplatform n'a pas de tâche `test`) ; utiliser `allUnitTests`.

Résultats JUnit XML : `sharedLogic/build/test-results/{testAndroidHostTest,iosSimulatorArm64Test}/`, `androidApp/build/test-results/testDebugUnitTest/`.

### Où sont les tests

- `sharedLogic/src/commonTest/kotlin/com/dmb/jobtracker/` : exécutés sur **deux runtimes** (JVM Android host test et Kotlin/Native iOS Simulator) — use cases, mappers, convertisseurs, repository (avec `FakeJobOfferDao`), ViewModel (`kotlinx-coroutines-test` + Turbine), graphe Koin (`DiModulesTest`, `databaseModule` remplacé par un DAO fake), logique de formulaire (`presentation/form/JobOfferFormLogicTest`, 74 cas, dont la validation `validate`), code template. Fixtures et fakes dans `testutil/` (`jobOffer(...)`, `jobOfferEntity(...)`, `FakeJobOfferRepository`, `FakeJobOfferDao`).
- `sharedLogic/src/iosTest/kotlin/com/dmb/jobtracker/data/local/` (**natif iOS uniquement**, SQLite réel via `BundledSQLiteDriver`) : `JobOfferDaoTest` (DAO sur base en mémoire : insert/update/delete, `@Query` de tri et de filtre par statut, Flow réémis), `JobOfferRepositoryRoomTest` (repository de production sur DAO réel), `MigrationTest` (migration 1 → 2 avec `MigrationTestHelper` de **room-testing 2.8.5**, base fichier créée depuis `schemas/…/1.json` puis validée contre `2.json`, données préservées, colonnes ajoutées à `NULL`, et ouverture par le vrai `getRoomDatabase`). Support : `RoomTestSupport.kt`.
- `androidApp/src/test/kotlin/` (JUnit 4 + `kotlin-test-junit`) : `TextCase`, `DateFormatting`, `OfferListLogic` (recherche + tri), `RestoredOfferScrollTest` (décision de défilement après « Annuler »), `DatePickerConversionsTest` (conversions du DatePicker Material, propres à Android), `FormSheetUsesSharedLogicTest` (garde-fou : l'écran Android appelle la logique partagée — dont `validate`, `validation.isValid`, `validation.errorMessage` — et ne réimplémente rien), `StatusLabels`, `SortOption`, contraste WCAG des couleurs de statut (`StatusColorsTest`).
- `iosApp/iosAppTests/` (XCTest, cible **hébergée par l'app**, schéma partagé `iosAppTests`) : `String+Case`, `FlowLayout.arrange`, `LocalDate+Bridge`, `JobOfferListLogic`, `JobOfferFormBridgeTests` (l'écran iOS appelle la logique partagée : conversion des dates, délégation, garde-fou sur le source), libellés / couleurs de statut (WCAG), format de date de la carte, `FontRegistrationTests` (les 4 polices Plus Jakarta Sans sont présentes et enregistrées), `KoinBridgeTests` (`KoinHelper` + `JobOfferListObservable`).
- Convention de nommage : `fonction_condition_résultatAttendu` (Kotlin) / `test_fonction_condition_résultatAttendu` (Swift). Les cas sont volontairement identiques Android/iOS (mêmes tableaux d'attendus) pour qu'une divergence de logique soit détectée.

### Périmètre de la couverture (Kover)

Le chiffre principal mesure la **logique** : sont exclus le code généré Room (`*_Impl`, `AppDatabaseConstructor`), les fonctions `@Composable`, `MainActivity` et `MonApplication` (le rendu n'est pas testé unitairement). Les exclusions du rapport agrégé sont définies dans le `build.gradle.kts` **racine** (celles des modules ne concernent que leur rapport propre). Instantané au 2026-09-20 : **82,5 % des lignes (292/354)**, 96,9 % des branches sur ce périmètre. Tout ce qui est logique métier (`domain`, `data.mapper`, `data.repository`, `presentation`, `di`, `ui.util`, `ui.joboffer` non Compose) est à 100 % ; le reste est l'amorçage Room (testé en natif, non mesuré par Kover), `Theme.kt`/`Type.kt` et `JobOfferListEvent` (classe vide).

### Décisions à ne pas refaire par erreur

- **`kotlinx-coroutines-test` reste à 1.10.1**, aligné sur `kotlinx-coroutines-core` (même `version.ref`) ; Turbine 1.2.1 tire coroutines 1.10.2 dans le seul classpath de test.
- **Les fakes de test sont `internal`** : `exposedSeverity = ERROR` (KMP-NativeCoroutines) s'applique aussi aux sources de test ; une classe publique exposant un `Flow`/`StateFlow` ne compile pas pour la cible iOS.
- **Le ViewModel est testé avec `Dispatchers.setMain(StandardTestDispatcher())`** (il crée son scope sur `Dispatchers.Main`) et `advanceUntilIdle()` : aucune attente réelle, pas de test dépendant de l'horloge.
- **Cible XCTest hébergée par l'app** (`TEST_HOST` = `JobTracker.app` : les symboles Kotlin viennent de l'app hôte ; aucun avertissement de classes Kotlin dupliquées constaté au lancement des tests). Elle a été créée par script avec la gem `xcodeproj` (le projet utilise des groupes synchronisés : tout `.swift` ajouté dans `iosApp/iosAppTests/` est compilé automatiquement). `PRODUCT_NAME = $(TARGET_NAME)` est surchargé, sinon la cible hérite de `PRODUCT_NAME=JobTracker` du `.xcconfig`. Le module de l'app s'importe avec `@testable import JobTracker`.
- **Le schéma s'appelle `iosAppTests`** (partagé, dans `xcshareddata`) : un schéma utilisateur `iosApp` (dans `xcuserdata`, créé par Xcode, non versionné) masque un schéma partagé de même nom et n'a pas d'action de test.
- **Couverture Swift : lire avec prudence** : l'app hôte se lance pendant les tests, donc les vues SwiftUI (`JobOfferListView`, `JobOfferCard`…) apparaissent partiellement « couvertes » sans être vérifiées. Seuls les fichiers de logique (`String+Case`, `FlowLayout`, `LocalDate+Bridge`, `JobOfferListLogic`, `Color+Theme`, `JobOffer+Display`) reflètent des tests réels.
- **Tests Room = natifs iOS, pas Android instrumenté** : `iosTest` s'exécute par Gradle sans appareil ni émulateur, avec le même driver SQLite embarqué que la production. `Room.inMemoryDatabaseBuilder` n'existe pas dans `commonTest` (la cible Android exige un `Context`). Le dossier des schémas est transmis au processus du simulateur par `SIMCTL_CHILD_ROOM_SCHEMA_DIR` (Gradle, `sharedLogic/build.gradle.kts`) : `simctl spawn` ne relaie que les variables préfixées `SIMCTL_CHILD_`.
- **`KoinBridgeTests` utilise la vraie base de l'app** (limite assumée) : le constructeur du ViewModel est `internal` et Koin n'est pas exposé à Swift, donc on ne peut pas injecter de fake. Chaque test travaille avec une offre au titre unique (`XCTest-<uuid>`) et la supprime ; il suppose que l'app hôte a déjà démarré Koin (`iOSApp.init`). Ne jamais faire de `DELETE` global de la base d'un simulateur pour « nettoyer » : elle peut contenir des données saisies à la main.
- **Extraction pour tester le défilement après « Annuler »** : la décision est pure (`locateRestoredOffer`, `isItemFullyVisible` dans `OfferListLogic.kt`), l'effet (`LazyListState`, `withFrameNanos`, `animateScrollToItem`) reste dans `JobOfferListScreen`. Risque résiduel : le câblage de cet effet n'est pas couvert par un test automatique (il a été vérifié à la main sur émulateur).
- **`AppFontRegistry` est interne** (et non plus `private`) pour pouvoir tester l'enregistrement des polices.
- **Logique de formulaire unique** (`sharedLogic/.../presentation/form/JobOfferFormLogic.kt`, `object` appelé par `JobOfferFormSheet` Android et iOS) : plus aucune règle de parsing/validation dans les écrans. Côté iOS : `JobOfferFormLogic.shared.…` (Kotlin) + `JobOfferFormBridge.buildOffer` (seule la conversion `Date` → `LocalDate` est propre à iOS). Des tests « garde-fou » (`FormSheetUsesSharedLogicTest`, `JobOfferFormBridgeTests`) lisent le source des écrans et échouent si une règle y est réintroduite. **Ne pas réimplémenter ces règles dans un écran.** Comportements de référence (décisions produit) :
  1. Champ texte optionnel vide **ou blanc** → `null` (jamais « "  " » stocké ; sinon la carte afficherait une ligne de localisation vide).
  2. Titre / entreprise « vides » = `isBlank()` (espaces, tabulations, **retours à la ligne**) → refusés.
  3. Saisie du salaire : 4 caractères max, chiffres uniquement, saisie trop longue **ignorée en bloc** (identique Android et iOS, iOS appliquant le filtre via `onChange`).
  4. Enregistrement du salaire : min + max → `55k - 70k` ; min seul → `55k+` ; aucun → `null`. **Un max sans min n'est plus jamais écrit** (règle 6) : `composeSalaryRange` renvoie `null` dans ce cas, qui est refusé en amont.
  5. Lecture (édition) : les 2 formes écrites sont relues à l'identique ; texte libre hérité : sans tiret → min ; **un** tiret → gauche = min, droite = max (« -70k » = max seul, la position fait foi) ; **deux tirets ou plus → rejeté (champs vides), jamais deviné**. Le format « jusqu'à 70k » (jamais écrit en production, produit un temps par une build de test) reste **lu** comme max seul : sans cela il serait lu comme « min = 70 », un changement de sens silencieux ; lu comme max seul, il ouvre le formulaire avec le message de la règle 6.
  6. **Validation bloquante (décision produit révisée)** : un salaire **max rempli sans min** (min vide, blanc ou sans chiffre) fait échouer `JobOfferFormLogic.validate(title, company, salaryMin, salaryMax)` avec le message « Renseigne aussi le salaire minimum, ou laisse les deux champs vides. » (`SALARY_MIN_REQUIRED_MESSAGE`). Le résultat est un `FormValidation` : `Valid`, `MissingRequiredField` (titre/entreprise vides : bouton désactivé **sans message**, comme avant) ou `InvalidSalary(message)` ; les écrans lisent `isValid` et `errorMessage`. Le salaire est évalué avant les champs obligatoires pour que son message (le seul à afficher) apparaisse dès que le max est rempli. Le blocage disparaît en remplissant le min ou en vidant le max. **Câblage** : Android = bouton `enabled = validation.isValid`, message en rouge juste au-dessus du bouton (visible même formulaire défilé), champ « min » en `isError`, message annoncé par TalkBack (`liveRegion`) ; iOS = message rouge en pied de la section « Salaire » (convention SwiftUI/HIG) et bouton de la barre d'outils désactivé. Ancienne décision abandonnée : enregistrer le max seul sous « jusqu'à Xk » (rendait la donnée non lisible par le reste de l'app sans règle de lecture, et changeait le contrat de données).
- **Attention** : une offre au salaire ambigu (texte libre à plusieurs tirets) s'ouvre avec des champs de salaire vides et l'enregistrer efface ce salaire ; de même une offre stockée « jusqu'à 70k » ou « -70k » s'ouvre avec le message de validation (elle ne peut être enregistrée qu'après avoir complété le min ou vidé le max). Sans conséquence en production (seul le formulaire écrit ce champ, avec les 2 formes ci-dessus), mais à savoir pour des données injectées à la main.
- **Petites extractions faites pour rendre la logique testable (comportement inchangé)** : (la logique de formulaire, d'abord extraite par plateforme, a ensuite été unifiée dans sharedLogic, voir ci-dessous ; Android garde `ui/joboffer/DatePickerConversions.kt`, iOS `Features/JobOffer/JobOfferFormBridge.swift`) ; Android `ui/util/DateFormatting.kt`, `ui/joboffer/OfferListLogic.kt`, `ui/joboffer/StatusLabels.kt` ; iOS `Features/JobOffer/JobOfferListLogic.swift`, `FlowLayout.arrange(sizes:maxWidth:spacing:)` (fonction statique pure), `JobOfferCard.dateFormatter` (plus `private`).
- **Ne jamais laisser un test « qui passe pour rien »** : les tests clés ont été validés par mutation (défaut injecté dans le code de production → tests rouges → code restauré).

## 6. Décisions d'architecture à ne pas refaire par erreur

- **Visibilité `internal`** sur `JobOfferDao`, `JobOfferRepositoryImpl`, `UpdateJobOfferUseCase`, `AppDatabase.jobOfferDao()` et le constructeur
  de `JobOfferListViewModel` : limite ce qui est exposé au framework Objective-C/Swift (bridging ObjC). Ne pas repasser en `public` sans raison.
- **ViewModel en Kotlin pur** (pas d'`androidx.lifecycle.ViewModel`) : consommable depuis Swift sans dépendance Android ; le scope est annulé via `onCleared()`.
- **KMP-NativeCoroutines en mode strict** : `exposedSeverity = ERROR` → tout `Flow`/`suspend` exposé doit être annoté (`@NativeCoroutines` / `@NativeCoroutinesState`).
  Côté Swift le state est observé avec `asyncSequence(for: viewModel.stateFlow)` (`JobOfferListObservable`).
- **iOS — barre de recherche** : `.searchable(... placement: .navigationBarDrawer(displayMode: .always))` est volontaire (« force la recherche sous le titre,
  comportement prévisible »). Ne pas y toucher sans validation. Le `.searchable` est attaché à la `List` (donc absent tant qu'il n'y a aucune offre).
- **iOS — nav bar** : on utilise `.tint(Color.tealPrimary)` plutôt que `toolbarBackground` / `toolbarColorScheme` (commentaire dans `JobOfferListView`) ; la barre reste
  système (pas de barre teal comme Android). La raison exacte liée à iOS 26 n'est pas documentée dans le code.
- **iOS — polices** : `.ttf` statiques (et non la police variable) car `.weight()` sur une police variable est peu fiable sur iOS ; enregistrement runtime pour ne
  pas modifier `Info.plist`/`project.pbxproj`.
- **iOS — couleurs adaptatives** via `UIColor` dynamique : suit le mode système comme `isSystemInDarkTheme()` côté Android.
- **Android — état de swipe non sauvegardable** (`SwipeableJobOfferItem.kt`) : l'état est créé avec `remember { SwipeToDismissBoxState(...) }` et **non** `rememberSwipeToDismissBoxState`, qui est un `rememberSaveable`.
  Dans un `LazyColumn` à clés (`key = { it.id }`), l'état sauvegardé d'un item retiré est restauré quand un item de même clé réapparaît : après « Annuler », la carte revenait à l'état `EndToStart` (décalée hors écran, fond `errorContainer` visible). Ne pas revenir à `rememberSwipeToDismissBoxState` dans une liste à clés où une offre peut réapparaître avec le même id.
- **Android — « Annuler » fait défiler jusqu'à la carte restaurée** (`JobOfferListScreen.kt`, `restoredOfferId`) : `LazyColumn` garde en place le 1er élément visible quand on insère au-dessus, donc une carte restaurée en tête de liste réapparaissait hors écran (ou à moitié visible). Après le ré-ajout, si la carte n'est pas entièrement visible, `animateScrollToItem`.
  `restoredOfferId` est une clé du `LaunchedEffect` : le remettre à `null` **à la fin** seulement (plus tôt, cela annule le scroll).
- **iOS — `listRowInsets` de la carte stats** : ne pas remettre `EdgeInsets()` (zéro) sur la `Section` de `JobOfferStatsCard` : la carte serait collée aux bords de l'écran et à la 1re candidature. Vérifier l'écran **réel** (base peuplée), pas seulement un `ScrollView` de test qui a sa propre marge.
- **Confirmation vs annulation de suppression** : Android = snackbar « Annuler » (guideline Material : pas de dialogue pour une suppression réversible) ; iOS = `confirmationDialog` (HIG : confirmer une action destructive, pas de toast standard). Différence volontaire.
- **Statuts = variantes clair/sombre validées par calcul de contraste** (cf. §4) ; l'opacité de teinte des badges dépend du mode. Ne pas revenir à une couleur unique.
- **Dates** : format `j mmm` français des deux côtés. kotlinx-datetime 0.6.1 n'a **pas** de `MonthNames` français intégrés (seulement `ENGLISH_*`) : `JobOfferCard.kt` définit ses 12 abréviations (`janv.`, `févr.`, `mars`, `avr.`, `mai`, `juin`, `juil.`, `août`, `sept.`, `oct.`, `nov.`, `déc.`),
  identiques à `DateFormatter` `fr_FR` sur iOS. `dayOfMonth(Padding.NONE)` : sans zéro initial (le défaut `Padding.ZERO` donnait « 05 »).
- `LocalDate` Kotlin ↔ `Date` Swift : conversions dans `Core/Extensions/LocalDate+Bridge.swift`.

## 7. Écarts connus entre Android et iOS

| Écart | Raison |
|---|---|
| Barre de titre : Android = TopAppBar teal (titre `titleLarge` blanc) ; iOS = grand titre système, police système | Le titre de navigation ne se stylise pas en SwiftUI sans `UINavigationBarAppearance` global ; approche abandonnée précédemment (voir §6). |
| Barre de recherche : `OutlinedTextField` Android sous la carte stats ; `.searchable` natif iOS | Décision explicite : ne pas modifier la recherche iOS. |
| FAB : carré arrondi Material (Android) vs cercle (iOS) ; ombre système | Non repris (hors périmètre), convention iOS conservée. |
| Icônes : Material (`Edit`, `LocationOn`, `ArrowDropDown`) vs SF Symbols (`pencil`, `mappin.and.ellipse`, `arrowtriangle.down.fill`) | Pas d'équivalent pixel-exact ; approximations natives les plus proches. |
| Interligne : sur une ligne = plancher (`frame(minHeight:)`) ; multi-lignes = `lineSpacing` | SwiftUI n'a pas de `lineHeight` ; approximation, écart possible ≤ 1–2 pt. |
| Feuilles de menu de statut : `DropdownMenu` Material vs `Menu` iOS | Composant système imposé par la plateforme. |
| Swipe de suppression : fond `errorContainer` (Android) vs action rouge système (iOS) | `swipeActions` impose son rendu. |
| Chip : pas d'état pressed/ripple identique | Comportement de feedback propre à chaque plateforme. |
| Suppression : snackbar « Annuler » (Android) vs boîte de confirmation (iOS) | Convention de chaque plateforme (voir §6). |
| Zone tactile du chip : 48 dp automatiques (M3) vs 44 pt explicites (iOS, chip visuel 32) | Minimums de chaque guideline (Material 48 dp / HIG 44 pt). |
| Icônes : taille fixe en dp sur Android, elles suivent Dynamic Type sur iOS | Sur Android l'échelle de police ne touche que le texte (comportement Material standard). |
| Police Android sur émulateur sans Google Play Services : repli sur Roboto | La police Plus Jakarta Sans passe par Google Fonts téléchargeable (`font_certs.xml`) : à vérifier sur appareil réel. |
