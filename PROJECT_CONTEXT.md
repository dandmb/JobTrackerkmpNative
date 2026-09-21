# JobLog — contexte projet

> Ce fichier reflète l'état **actuel** du projet (pas son historique). À mettre à jour à chaque
> intervention : ajouter ce qui change, retirer ce qui n'est plus vrai. **Voir §0 : compte rendu obligatoire sur le Desktop à la fin de chaque intervention.**
> Dernière mise à jour : 2026-09-21 (logo remplacé : carnet + coche à la place de la mallette, §9 ; RENOMMAGE en JobLog, nouveau logo, internationalisation anglais/français — §9 et §10 ; CI GitHub Actions ajoutée, §8 ; « Annuler » une suppression restaure la position et les valeurs d'origine ; audit UI complet de l'écran principal : centrage, champs de date, icônes de barre d'état, mise en page en police agrandie ; action de tri masquée quand aucune candidature n'existe ; icône d'état vide = boîte/plateau vide sur les deux plateformes ; règle permanente « Process de documentation » ajoutée en §0 ; écran « À propos » : données, droits, « Supprimer toutes mes données » en double confirmation, contact par e-mail ; Android en Material 3 Expressive ; icône d'état vide Android).

## 0. Process de documentation (règle permanente)

> **Règle du propriétaire, à relire et appliquer à CHAQUE intervention, sans qu'il ait à la rappeler :**
>
> À la fin de chaque intervention de développement sur ce projet, écris systématiquement un compte rendu dans un fichier markdown sur le Desktop (`~/Desktop/rapport-<nom-court-de-l-intervention>.md`), avec le même niveau de détail que les rapports précédents (fichiers créés/modifiés, comportement implémenté, tests ajoutés et leur résultat d'exécution, vérifications manuelles faites et restantes, écarts ou décisions prises signalés explicitement). Ne donne jamais le compte rendu uniquement dans le terminal — l'utilisateur récupère ce fichier pour le transmettre ailleurs.

Checklist de fin d'intervention :
1. `~/Desktop/rapport-<nom-court>.md` écrit (nom court en minuscules, mots séparés par des tirets ; ex. `rapport-a-propos.md`, `rapport-onboarding-splash.md`). Si l'intervention prolonge une précédente, on peut mettre à jour le rapport existant **à condition qu'il reste lisible seul** ; sinon en créer un nouveau.
2. Contenu minimal du rapport : fichiers créés / modifiés ; comportement implémenté ; tests ajoutés **et résultat d'exécution** (commandes, nombres de tests par suite, 0 échec / régressions) ; vérifications manuelles **faites** et **restantes** (souvent iOS) ; écarts et décisions signalés explicitement (y compris les prémisses de la demande qui se sont révélées inexactes) ; liste « à commiter ».
3. Le message final dans le terminal ne remplace jamais le fichier : il pointe vers lui et ne reprend que l'essentiel.
4. `PROJECT_CONTEXT.md` mis à jour (ce fichier reflète l'état **actuel**, pas l'historique).
5. Aucun commit git (le propriétaire commite lui-même).

## 1. Architecture générale

Application de suivi de candidatures. **Kotlin Multiplatform + MVVM + Room (KMP) + Koin**, avec des
**UI 100 % natives** : Jetpack Compose sur Android, SwiftUI sur iOS. Les deux UI consomment le même
ViewModel partagé.

| Module | Rôle |
|---|---|
| `sharedLogic` | Logique partagée (KMP : `androidMain`, `commonMain`, `iosMain`). Domaine, use cases, repository, Room, Koin, `JobOfferListViewModel`. Compilé en framework iOS statique `SharedLogic` (`iosArm64` + `iosSimulatorArm64`). |
| `androidApp` | App Android : Compose + Material3, thème, écrans (`ui/joboffer`, `ui/onboarding`, `ui/about`, `ui/AppRoot.kt`, `ui/theme`, `ui/util`). |
| `iosApp` | App iOS : SwiftUI (`iosApp/iosApp/Core`, `Features/App`, `Features/Splash`, `Features/Onboarding`, `Features/About`, `Features/JobOffer`, `Resources/Fonts`) + tests XCTest (`iosApp/iosAppTests`). Xcode lance `./gradlew :sharedLogic:embedAndSignAppleFrameworkForXcode` avant de compiler. Groupes Xcode « synchronisés » : tout fichier ajouté dans `iosApp/iosApp/` est inclus automatiquement dans la cible. |
| `sharedUI` | Module Compose Multiplatform **quasi vide** (`App.kt` du template). Non utilisé par les écrans réels : l'UI Android vit dans `androidApp`. |

Couches dans `sharedLogic` (`com.dmb.joblog`) : `domain` (model, repository, usecase) →
`data` (Room : entity/dao/converter/migrations, mapper, `JobOfferRepositoryImpl`) → `presentation`
(`JobOfferListViewModel`, `JobOfferListState`, `onboarding/`, `splash/`, `about/`) → `di` (modules Koin : repository, use case, viewModel, database, **onboarding**).
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
| Material3 (`org.jetbrains.compose.material3`) | 1.12.0-alpha03 — **tire `androidx.compose.material3:material3` 1.5.0-alpha22** (résolu par Gradle, vérifié avec `dependencyInsight`), qui contient déjà les API Material 3 Expressive (opt-in `@ExperimentalMaterial3ExpressiveApi` requis). Dernier stable androidx : 1.4.0 ; dernier alpha : 1.5.0-alpha28 (Google Maven, 2026-09-21). Pas de Compose BOM dans le projet (le BOM 2026.09.00 donnerait material3 1.4.0). |
| Room (runtime/compiler/plugin) | 2.8.5 |
| AndroidX SQLite (bundled) | 2.7.1 |
| Koin (core / android / compose) | 4.2.2 |
| KMP-NativeCoroutines | 1.0.6 (Android : plugin Gradle ; iOS : package SPM `KMPNativeCoroutines`, branche `master`, + RxSwift 6.10.2 résolu) |
| kotlinx-coroutines (core et test, alignés) | 1.10.1 |
| multiplatform-settings (`-no-arg`, et `-test` pour `MapSettings`) | 1.3.0 (vérifiée le 2026-09-20 sur les métadonnées Maven Central) |
| androidx.core:core-splashscreen | 1.2.0 (vérifiée le 2026-09-20 sur Google Maven) |
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
fourni par `JobLogTheme` dans `Theme.kt` ; iOS → `iosApp/Core/Theme/Color+Theme.swift` (`Color(light:dark:)`, `statusBadgeTintAlpha(for:)`),
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
  Postulé / Entretien / Résultat (label MAJUSCULES 11 medium, tracking 0.5, onSurfaceVariant ; valeur labelLarge SemiBold, format `j mmm` selon la langue (`ShortDate`, §10) : FR « 5 sept. », « 12 févr. » / EN « Sep 5 ») ; salaire `💰 …`.
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
- **Splash + onboarding (3 écrans)**, Android et iOS. Au premier lancement : splash → onboarding → écran principal (sans redémarrage) ; ensuite : splash → écran principal direct. Le drapeau « onboarding vu » est stocké par multiplatform-settings (clé `onboarding_completed` ; Android = SharedPreferences par défaut, iOS = NSUserDefaults). Voir §6 « Splash et onboarding ».
- **Écran « À propos »** (icône ⓘ dans la barre du haut de la liste, Android et iOS) : ce que l'app enregistre, où vivent les données, ce qu'elle ne fait pas, droits de l'utilisateur, **« Supprimer toutes mes données »** (double confirmation, vide la table `job_offers`), version (lue dans la config de build), lien « Nous contacter » (`mailto:` avec objet pré-rempli). **Aucune licence tierce n'y est mentionnée (décision explicite du propriétaire).** Texte informatif de bonne foi, **pas un document juridique** : voir §6 « Écran À propos ».

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

- `sharedLogic/src/commonTest/kotlin/com/dmb/joblog/` : exécutés sur **deux runtimes** (JVM Android host test et Kotlin/Native iOS Simulator) — use cases, mappers, convertisseurs, repository (avec `FakeJobOfferDao`), ViewModel (`kotlinx-coroutines-test` + Turbine), graphe Koin (`DiModulesTest`, `databaseModule` remplacé par un DAO fake), logique de formulaire (`presentation/form/JobOfferFormLogicTest`, 74 cas, dont la validation `validate`), code template. Fixtures et fakes dans `testutil/` (`jobOffer(...)`, `jobOfferEntity(...)`, `FakeJobOfferRepository`, `FakeJobOfferDao`).
- `sharedLogic/src/iosTest/kotlin/com/dmb/joblog/data/local/` (**natif iOS uniquement**, SQLite réel via `BundledSQLiteDriver`) : `JobOfferDaoTest` (DAO sur base en mémoire : insert/update/delete, `@Query` de tri et de filtre par statut, Flow réémis), `JobOfferRepositoryRoomTest` (repository de production sur DAO réel), `MigrationTest` (migration 1 → 2 avec `MigrationTestHelper` de **room-testing 2.8.5**, base fichier créée depuis `schemas/…/1.json` puis validée contre `2.json`, données préservées, colonnes ajoutées à `NULL`, et ouverture par le vrai `getRoomDatabase`). Support : `RoomTestSupport.kt`.
- `androidApp/src/test/kotlin/` (JUnit 4 + `kotlin-test-junit`) : `TextCase`, `DateFormatting`, `OfferListLogic` (recherche + tri), `RestoredOfferScrollTest` (décision de défilement après « Annuler »), `DatePickerConversionsTest` (conversions du DatePicker Material, propres à Android), `FormSheetUsesSharedLogicTest` (garde-fou : l'écran Android appelle la logique partagée — dont `validate`, `validation.isValid`, `validation.errorMessage` — et ne réimplémente rien), `StatusLabels`, `SortOption`, contraste WCAG des couleurs de statut (`StatusColorsTest`).
- `iosApp/iosAppTests/` (XCTest, cible **hébergée par l'app**, schéma partagé `iosAppTests`) : `String+Case`, `FlowLayout.arrange`, `LocalDate+Bridge`, `JobOfferListLogic`, `JobOfferFormBridgeTests` (l'écran iOS appelle la logique partagée : conversion des dates, délégation, garde-fou sur le source), libellés / couleurs de statut (WCAG), format de date de la carte, `FontRegistrationTests` (les 4 polices Plus Jakarta Sans sont présentes et enregistrées), `KoinBridgeTests` (`KoinHelper` + `JobOfferListObservable`).
- **Splash / onboarding** : `OnboardingRepositoryImplTest` (10, `MapSettings` en mémoire), `OnboardingViewModelTest` (8, `FakeOnboardingRepository`), `OnboardingContentTest` (15), `SplashGatingTest` (9) dans `commonTest` ; 4 tests Koin dans `DiModulesTest` (Settings remplacé par `MapSettings`) ; `SplashAndOnboardingWiringTest` (7 gardes qui lisent le source Android) ; `OnboardingBridgeTests` (14, Swift : pont Koin, contenu, `SplashGating`, gardes de câblage sur le source iOS).
- **À propos** : `AboutContentTest` (33), `AboutViewModelTest` (21), `DeleteAllJobOffersUseCaseTest` (5), `deleteAll_*` dans `JobOfferRepositoryImplTest` (+4) et `DiModulesTest` (+2) — tous en `commonTest` (JVM **et** natif) ; `deleteAll_*` sur SQLite réel dans `JobOfferDaoTest` (+4) et `JobOfferRepositoryRoomTest` (+1) ; `EmptyStateIconWiringTest` (2, icône de l'état vide Android, au-dessus du titre, décorative et atténuée) ; `AboutScreenWiringTest` (15, Android : câblage, contact, pas de licence à l'écran, faits de confidentialité) ; `AboutBridgeTests` (19, Swift : pont Koin, contenu, contact, version = `Config.xcconfig`, câblage, faits de confidentialité).
- **Audit UI de l'écran principal** : `ListContentCenteringWiringTest` (3), `ListLayoutWiringTest` (6), `DatePickerFieldWiringTest` (3), `StatusBarIconsWiringTest` (3) — Android, lecture du source.
- **Annuler une suppression** : `UndoDeleteWiringTest` (3, Android) ; `JobOfferListViewModelUndoTest` (8), `UndoDeletionOrderTest` (8) et `DeleteJobOfferUseCaseTest` (+7) en `commonTest` ; 3 tests sur SQLite réel dans `JobOfferRepositoryRoomTest` (natif).
- **Visibilité de l'action de tri** : `SortActionVisibilityWiringTest` (4, Android) ; 3 tests de la toolbar dans `EmptyStateWiringTests` (iOS).
- **État vide** : `EmptyStateIconWiringTest` (3, Android : `Icons.Outlined.Inbox` avant le titre, décorative/atténuée/48 dp, plus de `WorkOutline`) et `EmptyStateWiringTests` (5, iOS : `ContentUnavailableView` + `tray` dans la branche « liste vide » avant la `List`, sous-titre conservé, « aucun résultat » sans icône, `.searchable` intact, plus de `briefcase`). **Expressive** : 2 gardes dans `AboutScreenWiringTest` (composants utilisés ; non-débordement hors de l'écran « À propos »).
- **Rebranding / i18n (2026-09-21)** : voir §10 pour la liste des tests de localisation. **Totaux de référence après cette intervention** : Android `androidApp:testDebugUnitTest` 161, `sharedLogic:testAndroidHostTest` 342, `sharedLogic:iosSimulatorArm64Test` 388 (natif, incl. Room), XCTest `iosAppTests` 176 ; 0 échec. Kover non relancé après cette intervention (les chiffres de couverture ci-dessous datent d'avant).
- Convention de nommage : `fonction_condition_résultatAttendu` (Kotlin) / `test_fonction_condition_résultatAttendu` (Swift). Les cas sont volontairement identiques Android/iOS (mêmes tableaux d'attendus) pour qu'une divergence de logique soit détectée.

### Périmètre de la couverture (Kover)

Le chiffre principal mesure la **logique** : sont exclus le code généré Room (`*_Impl`, `AppDatabaseConstructor`), les fonctions `@Composable`, `MainActivity` et `MonApplication` (le rendu n'est pas testé unitairement). Les exclusions du rapport agrégé sont définies dans le `build.gradle.kts` **racine** (celles des modules ne concernent que leur rapport propre). Instantané au 2026-09-21 (après « À propos » et contact) : **85,7 % des lignes (424/495)**, 95,7 % des branches (178/186) sur ce périmètre (avant : 83,6 % — 321/384 — et 97,0 %) ; les 8 lignes non couvertes ajoutées sont `AppVersion.kt` et `ContactIntent.kt` (dépendent de `Context`/`Intent`, vérifiés à l'écran). **Piège Kover** : après un `./gradlew … --rerun-tasks` incluant les tâches Kover, le rapport agrégé est ressorti *non filtré* (brut 51 % : classes Room `*_Impl` et Composables inclus) et les relances suivantes le resservaient depuis le cache ; le rapport correct s'obtient avec `./gradlew koverXmlReport koverHtmlReport` **sans `--rerun-tasks`** (les tests, eux, se relancent avec `allUnitTests --rerun-tasks`). Tout ce qui est logique métier (`domain`, `data.mapper`, `data.repository`, `presentation`, `di`, `ui.util`, `ui.joboffer` non Compose, `presentation.onboarding`, `presentation.splash`, `presentation.about`) est à 100 % des lignes ; le reste est l'amorçage Room (testé en natif, non mesuré par Kover), `Theme.kt`/`Type.kt` et `JobOfferListEvent` (classe vide).

### Décisions à ne pas refaire par erreur

- **`kotlinx-coroutines-test` reste à 1.10.1**, aligné sur `kotlinx-coroutines-core` (même `version.ref`) ; Turbine 1.2.1 tire coroutines 1.10.2 dans le seul classpath de test.
- **Les fakes de test sont `internal`** : `exposedSeverity = ERROR` (KMP-NativeCoroutines) s'applique aussi aux sources de test ; une classe publique exposant un `Flow`/`StateFlow` ne compile pas pour la cible iOS.
- **Le ViewModel est testé avec `Dispatchers.setMain(StandardTestDispatcher())`** (il crée son scope sur `Dispatchers.Main`) et `advanceUntilIdle()` : aucune attente réelle, pas de test dépendant de l'horloge.
- **Cible XCTest hébergée par l'app** (`TEST_HOST` = `JobLog.app` : les symboles Kotlin viennent de l'app hôte ; aucun avertissement de classes Kotlin dupliquées constaté au lancement des tests). Elle a été créée par script avec la gem `xcodeproj` (le projet utilise des groupes synchronisés : tout `.swift` ajouté dans `iosApp/iosAppTests/` est compilé automatiquement). `PRODUCT_NAME = $(TARGET_NAME)` est surchargé, sinon la cible hérite de `PRODUCT_NAME=JobLog` du `.xcconfig`. Le module de l'app s'importe avec `@testable import JobLog`.
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
  6. **Validation bloquante (décision produit révisée)** : un salaire **max rempli sans min** (min vide, blanc ou sans chiffre) fait échouer `JobOfferFormLogic.validate(title, company, salaryMin, salaryMax)` avec le message « Renseigne aussi le salaire minimum, ou laisse les deux champs vides. » (`salaryMinRequiredMessage(language)`). Le résultat est un `FormValidation` : `Valid`, `MissingRequiredField` (titre/entreprise vides : bouton désactivé **sans message**, comme avant) ou `InvalidSalary(message)` ; les écrans lisent `isValid` et `errorMessage`. Le salaire est évalué avant les champs obligatoires pour que son message (le seul à afficher) apparaisse dès que le max est rempli. Le blocage disparaît en remplissant le min ou en vidant le max. **Câblage** : Android = bouton `enabled = validation.isValid`, message en rouge juste au-dessus du bouton (visible même formulaire défilé), champ « min » en `isError`, message annoncé par TalkBack (`liveRegion`) ; iOS = message rouge en pied de la section « Salaire » (convention SwiftUI/HIG) et bouton de la barre d'outils désactivé. Ancienne décision abandonnée : enregistrer le max seul sous « jusqu'à Xk » (rendait la donnée non lisible par le reste de l'app sans règle de lecture, et changeait le contrat de données).
- **Attention** : une offre au salaire ambigu (texte libre à plusieurs tirets) s'ouvre avec des champs de salaire vides et l'enregistrer efface ce salaire ; de même une offre stockée « jusqu'à 70k » ou « -70k » s'ouvre avec le message de validation (elle ne peut être enregistrée qu'après avoir complété le min ou vidé le max). Sans conséquence en production (seul le formulaire écrit ce champ, avec les 2 formes ci-dessus), mais à savoir pour des données injectées à la main.
- **Petites extractions faites pour rendre la logique testable (comportement inchangé)** : (la logique de formulaire, d'abord extraite par plateforme, a ensuite été unifiée dans sharedLogic, voir ci-dessous ; Android garde `ui/joboffer/DatePickerConversions.kt`, iOS `Features/JobOffer/JobOfferFormBridge.swift`) ; Android `ui/util/DateFormatting.kt`, `ui/joboffer/OfferListLogic.kt`, `ui/joboffer/StatusLabels.kt` ; iOS `Features/JobOffer/JobOfferListLogic.swift`, `FlowLayout.arrange(sizes:maxWidth:spacing:)` (fonction statique pure), `JobOfferCard.dateFormatter` (plus `private`).
- **Kotlin/Native : l'instance de la classe de test est partagée entre les tests, et les `single` des modules Koin top-level (`repositoryModule`) survivent d'un test à l'autre** : un test qui compte des éléments via le vrai `repositoryModule` voit les données des autres tests (constaté sur `aboutViewModelFromKoin_…`, qui passait en JVM et échouait en natif). Dans un `runTest` commun, construire ses propres `single` (DAO fake + repository) dans le test, et comparer des titres plutôt que des tailles quand l'état peut être partagé.
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

### Splash et onboarding

- **Persistance** : `OnboardingRepository` (interface publique : `hasCompletedOnboarding()`, `setOnboardingCompleted()`) ; `OnboardingRepositoryImpl` **internal**, sur `com.russhwolf.settings.Settings` (clé `ONBOARDING_COMPLETED_KEY = "onboarding_completed"`, défaut `false`). `onboardingModule` (`di/OnboardingModule.kt`, dans `sharedModules()`) déclare `Settings()` (variante no-arg), le repository (single) et `OnboardingViewModel` (factory, **constructeur internal**). iOS : `KoinHelper().onboardingViewModel()`. Les tests remplacent `Settings` par `MapSettings`.
- **Règles pures partagées (ajout par rapport au cahier des charges, à connaître)** : `SplashGating` (`MIN_DURATION_MILLIS = 800`, `shouldKeepSplash(elapsedMillis, isLoading) = elapsed < 800 || isLoading`) et `OnboardingContent` (3 pages « Suis tes candidatures » / « Garde un œil sur les statuts » / « Vois où tu en es », libellés « Passer » / « Suivant » / « Commencer », `showsSkip`, `primaryButtonLabel`, `nextPageIndex`) vivent dans `sharedLogic` et sont appelées par les deux UI : le gating et le texte sont **identiques par construction**. Côté Swift, `OnboardingPage.description` s'appelle `description_` et les `Int` Kotlin sont des `Int32`.
- **Gating du splash** : le splash reste tant que la durée minimale n'est pas écoulée OU que `JobOfferListViewModel.state.isLoading` est vrai. **Le ViewModel de la liste est obtenu UNE SEULE FOIS à la racine** (Android : `MainActivity` puis `AppRoot(...)` ; iOS : `AppRootModel` puis `JobOfferListView(viewModel:)`) — ne pas le ré-instancier dans un écran (iOS : `JobOfferListView` crée son `JobOfferListObservable` mais sur le même ViewModel Kotlin). Des gardes (`SplashAndOnboardingWiringTest`, `OnboardingBridgeTests`) lisent le source pour le vérifier ; ils échouent avec un message explicite si un fichier est renommé.
- **Android** : `androidx.core.splashscreen`, `installSplashScreen()` **avant** `super.onCreate()` puis `setKeepOnScreenCondition { SplashGating… }`. Thèmes : `Theme.App` (application) et `Theme.App.Starting` (parent `Theme.SplashScreen`, fond `splash_background` = teal `#0D6E68`, icône `drawable/ic_splash_logo.xml`, `postSplashScreenTheme = Theme.App`). **L'icône du splash est un PLACEHOLDER** (pictogramme « work » Material blanc) : les icônes de lanceur sont encore celles du template Android ; à remplacer par le vrai logo. Onboarding : `HorizontalPager` + indicateurs animés + « Passer » (masqué à la dernière page, place réservée) + bouton « Suivant » / « Commencer ». `AppRoot` mémorise `showOnboarding` en `rememberSaveable` (rotation) et bascule en `Crossfade` après `completeOnboarding()`.
- **iOS** : le storyboard LaunchScreen est **inchangé** (fond système) ; `SplashView` (SwiftUI, teal `0x0D6E68`, `JobLogMark` = logo carnet + coche, §9) prend le relais dans `AppRootView`, d'où une brève séquence blanc/système → teal → onboarding. `AppRootModel` (ObservableObject) crée les deux ViewModels une seule fois et publie `keepSplash`. Onboarding : `TabView` + `.tabViewStyle(.page(indexDisplayMode: .never))` avec indicateurs dessinés à la main (mêmes capsules qu'Android).
- **Police iOS** : l'onboarding utilise Plus Jakarta Sans (`.appTextStyle`), comme le reste de l'app iOS — et non la police système : la consigne « police système » ne correspondait pas à l'état réel du projet (polices embarquées sur iOS).
- **Tests XCTest** : ne **jamais** appeler `completeOnboarding()` dans un test iOS (cible hébergée : cela écrirait dans les vrais `UserDefaults` du simulateur et supprimerait l'onboarding au lancement suivant).
- **Test flaky corrigé au passage** : `JobOfferRepositoryRoomTest` (natif) utilisait un `delay` virtuel de `runTest` ; remplacé par un vrai `delay(5)` sur `Dispatchers.Default`.

### Écran « À propos »

- **Une seule source de vérité** : `sharedLogic/.../presentation/about/AboutContent.kt` (sections, libellés, textes des deux confirmations, licences) ; les écrans ne font que la mise en forme (gardes : `AboutScreenWiringTest`, `AboutBridgeTests`). `AboutContent.versionLabel(versionName, buildNumber)` formate ; **chaque plateforme fournit la version depuis sa config de build** : Android `PackageManager` (`versionName`/`versionCode` de `androidApp/build.gradle.kts`, `ui/about/AppVersion.kt`), iOS `Bundle.main` (`MARKETING_VERSION` / `CURRENT_PROJECT_VERSION` de `Configuration/Config.xcconfig`, `Features/About/AppVersion.swift`). Rien n'est codé en dur.
- **Suppression totale** : `JobOfferRepository.deleteAll()` → `JobOfferDao.deleteAll()` (`DELETE FROM job_offers`, une requête) → `DeleteAllJobOffersUseCase` → `AboutViewModel` (constructeur `internal`, factory Koin, `KoinHelper.aboutViewModel()`). **Double confirmation portée par le ViewModel** (`DeleteAllStep` : `IDLE` → `FIRST_CONFIRMATION` → `FINAL_CONFIRMATION` → suppression) : seule `onDeleteAllFinalConfirmed()` supprime, et elle est ignorée si l'étape 1 n'a pas eu lieu. Le flux Room de la liste se réémet vide tout seul (rien à faire côté `JobOfferListViewModel`). Échec → `errorMessage`, données conservées. **L'état d'onboarding (`onboarding_completed`) n'est PAS effacé** (préférence, pas une candidature). DELETE SQLite simple : pas d'effacement sécurisé ni `VACUUM`.
- **Ne jamais appeler `onDeleteAllFinalConfirmed()` dans un XCTest** (cible hébergée : cela viderait la vraie base du simulateur). Les tests iOS s'arrêtent avant (annulation).
- **Affirmations du texte = vérifiées dans le code, et verrouillées par des tests** : manifeste Android sans aucune permission (manifeste fusionné : seule la permission interne `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` d'AndroidX), aucun SDK d'analyse/pub/réseau dans `libs.versions.toml` ni dans les paquets Swift (seul `KMP-NativeCoroutines`, + RxSwift transitif), aucun appel réseau dans les sources Kotlin/Swift, aucun `*UsageDescription` iOS. Si un de ces faits change, `AboutScreenWiringTest` / `AboutBridgeTests` échouent : **relire `AboutContent` avant de continuer**. `AboutContentTest.jobOfferFields_…` verrouille aussi la liste des champs de `JobOffer` (un champ ajouté doit apparaître dans « Ce que l'app enregistre »).
- **Deux nuances déclarées dans le texte** : (1) Android : la police Plus Jakarta Sans est téléchargée par Google Play Services (police téléchargeable) — requête de Google, sans lien avec les candidatures ; (2) `android:allowBackup="true"` (défaut) et sauvegardes iCloud/appareil iOS : le système peut sauvegarder la base selon les réglages de l'utilisateur, et « Supprimer toutes mes données » n'efface pas une sauvegarde déjà faite. Décision ouverte : passer `allowBackup` à `false` (ou définir des règles d'extraction) n'a pas été fait (hors périmètre).
- **Statut du texte** : information de bonne foi rédigée à partir du code, **pas un document juridique vérifié**. Une publication sur App Store / Play Store exigera une vraie politique de confidentialité hébergée en ligne (et, si des utilisateurs européens sont visés, une analyse RGPD).
- **Licences : volontairement ABSENTES de l'écran** (décision explicite du propriétaire : ni bibliothèques open source, ni Plus Jakarta Sans / OFL). Ne pas les rétablir sans demande ; `AboutContentTest.text_mentionsNoThirdPartyLicenseNorLibraryNorFont`, `AboutScreenWiringTest.aboutScreen_mentionsNoLicenseNorThirdPartyElement` et `AboutBridgeTests.test_aboutView_mentionsNoLicenseNorThirdPartyElement` le verrouillent. (`OFL.txt` reste dans le bundle iOS : c'est la licence de la police embarquée, indépendante de cet écran.)
- **Contact** : constantes partagées `AboutContent.CONTACT_EMAIL` (`bizwadan@gmail.com`), `CONTACT_SUBJECT` (« JobLog - Contact »), `contactMailtoUri()` → `mailto:bizwadan@gmail.com?subject=JobLog%20-%20Contact` (encodage pourcent UTF-8 fait à la main, testé : espace, caractères réservés, accents, €, emoji). Android : `ui/about/ContactIntent.kt`, `Intent.ACTION_SENDTO` + cette URI, `ActivityNotFoundException` rattrapée (pas de `<queries>` nécessaire) ; iOS : `@Environment(\.openURL)` avec `completion` (`accepted == false` si aucun client mail, ex. simulateur sans Mail). Sans application de messagerie, l'adresse est affichée en clair (`contactNoMailAppMessage`). L'adresse et l'objet ne sont codés en dur nulle part ailleurs (gardes Android + Swift). Le texte précise que rien n'est envoyé tant que l'utilisateur ne l'envoie pas lui-même (vrai : `mailto:` ouvre un brouillon).
- **Material 3 Expressive (Android, écran « À propos » UNIQUEMENT)** : décision de périmètre — le thème global (`JobLogTheme`) et les autres écrans restent en Material 3 « classique ». Aucune dépendance modifiée (déjà en 1.5.0-alpha22, voir §2). Composants utilisés : `MaterialExpressiveTheme` (local à l'écran, reprend couleurs/formes/typographie de l'app) + `MotionScheme.expressive()` (ressorts) ; `LargeFlexibleTopAppBar` (grand titre qui se réduit au défilement, `exitUntilCollapsedScrollBehavior`) ; `Button(shapes = ButtonDefaults.shapes())` à `MediumContainerHeight` (forme qui se transforme à l'appui) ; `LoadingIndicator` (pendant la suppression) ; `MaterialShapes.Cookie9Sided` (pastille d'en-tête) ; cartes `shapes.extraLarge` sur `surfaceContainerLow`/`errorContainer` ; dialogues à icône, bouton de confirmation finale plein en `error` (première étape : bouton texte en `error`) ; espacements en multiples de 8 dp ; `AppRoot` ouvre l'écran avec `slideIn/OutHorizontally` + `fadeIn/Out` pilotés par les specs de `MotionScheme.expressive()`. Étendre Expressive au reste de l'app = changer `JobLogTheme` (`MaterialExpressiveTheme`) puis revoir les écrans ; le garde `expressive_isNotAppliedToOtherScreensInThisChange` devra alors être retiré. Les styles typographiques `*Emphasized` ne sont PAS utilisés (l'`AppTypography` du projet ne les définit pas : ils retomberaient sur la police par défaut, pas Plus Jakarta Sans).
- **Icône d'état vide = « boîte / plateau vide », pas une icône thématique** (décision UX, voir `rapport-fix-icone-etat-vide.md`) : Android `Icons.Outlined.Inbox`, iOS `tray`. Ne pas revenir à la mallette (`WorkOutline` / `briefcase`) pour l'état vide : elle évoque « travail », pas « rien ici ». La mallette reste l'icône de **marque** (splash, onboarding, pastille de l'en-tête « À propos »).
- **Action de tri = seulement s'il existe au moins une candidature** (Android : `IconButton` + `DropdownMenu` dans `if (state.offers.isNotEmpty())` ; iOS : `ToolbarItem` du `Menu` dans `if !observable.state.offers.isEmpty`). La condition porte sur **`state.offers` (toutes les offres)**, JAMAIS sur `visibleOffers` / la recherche : une recherche sans résultat avec des candidatures en base garde le tri visible. L'icône ⓘ « À propos » reste toujours visible. Android referme `sortMenuExpanded` quand la liste devient vide (`LaunchedEffect`) pour que le menu ne se rouvre pas seul au retour de données. Gardes : `SortActionVisibilityWiringTest` (4, Android) et 3 tests dans `EmptyStateWiringTests` (iOS). Historique : l'action de tri était inconditionnelle depuis le premier commit (`0c4a69b`).
- **Mise en page de l'écran principal Android (audit UI, `rapport-audit-ui-complet.md`)** : (1) le `Box` de contenu est `weight(1f).fillMaxWidth()` — SANS `fillMaxWidth`, un enfant `weight` d'une `Column` garde la largeur de son contenu et `Alignment.Center` ne centre rien (« Aucun résultat » collé à gauche, indicateur de chargement aussi) ; (2) la carte de stats est le **premier item de la `LazyColumn`** (`STATS_ITEM_COUNT = 1` décale les index de défilement après « Annuler »), seule la recherche reste épinglée ; (3) « aucune candidature » (`state.offers.isEmpty()`) est testé AVANT « aucun résultat » et la requête est effacée quand la liste devient vide ; les textes de l'état vide ont `textAlign = TextAlign.Center` (sinon un sous-titre sur 2 lignes s'aligne à gauche en police agrandie) ; (4) **champ de date** (`JobOfferFormSheet`) : ouverture du sélecteur via `interactionSource` + `PressInteraction.Release` — plus de couche transparente `offset(y = -56.dp)` (elle réservait 56 dp d'espace en trop sous chaque champ ET interceptait le bouton « Effacer », qui ouvrait le sélecteur au lieu d'effacer) ; (5) `StatusBarIconsForPrimaryTopBar()` (liste et « À propos ») : en thème sombre `primary` est un teal clair → icônes de barre d'état SOMBRES (avant : blanches, illisibles).
- **Constats de l'audit (le point « Annuler » a été CORRIGÉ depuis, voir `rapport-fix-position-annuler.md`)** : `.searchable` iOS placé en bas sur iOS 26 (décision protégée) ; le titre d'une candidature est mis en « Title Case » dans la carte (« Et Architecte ») mais affiché brut dans le snackbar « supprimée » ; libellés du sélecteur de date Material dans la langue du système (« Select date » sur un appareil en anglais) (alors que « Annuler / OK » étaient codés en français — résolu par l'i18n, §10 : tout suit désormais la langue du système) ; démarrage à froid de l'émulateur lent (splash ~15-25 s) — probablement l'émulateur, non mesuré sur appareil réel.
- **« Annuler » après suppression = restauration FIDÈLE** (`rapport-fix-position-annuler.md`) : avant, `onAddOffer` ré-insérait avec `createdAt = maintenant` → la candidature remontait en tête de liste. Maintenant : `DeleteJobOfferUseCase.invoke(offer)` lit `createdAt` AVANT de supprimer et renvoie un `DeletedJobOffer(offer, createdAtEpochMillis)` ; `JobOfferListViewModel` le garde EN MÉMOIRE (`recentlyDeleted`, 20 max, FIFO, sous `Mutex`) ; `onRestoreOffer(offer)` appelle `DeleteJobOfferUseCase.restore` → `JobOfferRepository.restore(offer, createdAt)` (`INSERT OR REPLACE` avec l'horodatage d'origine). Choix : la suppression reste RÉELLE en base tout de suite (pas de suppression différée : plus simple, et la liste/les stats se mettent à jour immédiatement) ; c'est la mémorisation de ce qui a été supprimé qui rend l'annulation exacte. Nouvelles méthodes de `JobOfferRepository` : `getCreatedAt(id)` et `restore(offer, createdAt)`. **`onDeleteOffer` est IDEMPOTENT** : le geste de glissement l'appelle **4 fois** pour une seule suppression (`confirmValueChange` de `SwipeToDismissBox` est rappelé) ; à partir du 2e appel la ligne n'existe plus (horodatage lu = null) et ne doit pas écraser l'horodatage d'origine (piège découvert à l'écran, invisible dans les tests tant que le fake gardait l'horodatage après suppression : `FakeJobOfferRepository` l'oublie désormais comme la vraie base). Sans suppression connue, `onRestoreOffer` retombe sur `addIfMissing` (ajout simple seulement si l'offre est absente de la base). **iOS** : aucun « Annuler » (le glissement ouvre un `confirmationDialog` AVANT de supprimer) : mécanisme non applicable, le ViewModel partagé n'y change rien.
- **Navigation** : Android = `AppRoot` superpose `AboutScreen` à la liste (`AnimatedVisibility` + `BackHandler`, la liste reste composée : recherche/tri/défilement conservés) ; iOS = `NavigationLink` dans la barre de la liste (pile de navigation ; `.searchable` non touché). Le garde de l'onboarding `listViewModel_isInstantiatedOnceAtTheRootAndPassedDown` cherche désormais `JobOfferListScreen(viewModel = jobOfferListViewModel,` (un paramètre `onOpenAbout` a été ajouté).

## 7. Écarts connus entre Android et iOS

| Écart | Raison |
|---|---|
| Barre de titre : Android = TopAppBar teal (titre `titleLarge` blanc) ; iOS = grand titre système, police système | Le titre de navigation ne se stylise pas en SwiftUI sans `UINavigationBarAppearance` global ; approche abandonnée précédemment (voir §6). |
| Barre de recherche : `OutlinedTextField` Android **épinglée en haut, au-dessus de la carte de stats qui défile avec la liste** (audit UI : épinglée, la carte mangeait ~60 % de l'écran en police 200 %) ; `.searchable` natif iOS (sous le titre ; sur iOS 26 il s'affiche en bas de l'écran, en surimpression — décision `.searchable` non modifiée) | Décision explicite : ne pas modifier la recherche iOS. |
| FAB : carré arrondi Material (Android) vs cercle (iOS) ; ombre système | Non repris (hors périmètre), convention iOS conservée. |
| Icônes : Material (`Edit`, `LocationOn`, `ArrowDropDown`) vs SF Symbols (`pencil`, `mappin.and.ellipse`, `arrowtriangle.down.fill`) | Pas d'équivalent pixel-exact ; approximations natives les plus proches. |
| Interligne : sur une ligne = plancher (`frame(minHeight:)`) ; multi-lignes = `lineSpacing` | SwiftUI n'a pas de `lineHeight` ; approximation, écart possible ≤ 1–2 pt. |
| Feuilles de menu de statut : `DropdownMenu` Material vs `Menu` iOS | Composant système imposé par la plateforme. |
| Swipe de suppression : fond `errorContainer` (Android) vs action rouge système (iOS) | `swipeActions` impose son rendu. |
| Chip : pas d'état pressed/ripple identique | Comportement de feedback propre à chaque plateforme. |
| Suppression : snackbar « Annuler » (Android) vs boîte de confirmation (iOS) | Convention de chaque plateforme (voir §6). |
| Zone tactile du chip : 48 dp automatiques (M3) vs 44 pt explicites (iOS, chip visuel 32) | Minimums de chaque guideline (Material 48 dp / HIG 44 pt). |
| Icônes : taille fixe en dp sur Android, elles suivent Dynamic Type sur iOS | Sur Android l'échelle de police ne touche que le texte (comportement Material standard). |
| Splash : Android = SplashScreen système (teal dès le lancement) ; iOS = LaunchScreen système puis `SplashView` teal | LaunchScreen laissé tel quel sur demande ; un flash blanc/système précède donc le splash teal sur iOS. |
| Écran « À propos » : Android en Material 3 Expressive (grand titre, cartes à grands arrondis, formes de bouton animées) ; iOS en SwiftUI standard (`ScrollView`, `NavigationStack`) | Expressive est spécifique à Material ; pas d'équivalent HIG à reproduire. |
| État vide de la liste : Android = `Icons.Outlined.Inbox` (boîte/plateau vide, 48 dp, `onSurfaceVariant`, décorative) ; iOS = `ContentUnavailableView` avec SF Symbol `tray` | Même famille conceptuelle « plateau vide » (convention : l'exemple « No Mail » d'Apple utilise `tray.fill` ; Material 1 demande une image discrète et neutre) ; assets natifs de chaque plateforme, pas de parité pixel. **Seulement pour « aucune candidature du tout »** : « aucun résultat de recherche » reste du texte seul sur les deux plateformes. |
| À propos : Android = écran superposé, dialogues Material `AlertDialog` (×2) ; iOS = push de navigation, `confirmationDialog` (étape 1) puis `alert` (étape finale, présentée avec 0,4 s de délai) | Conventions de chaque plateforme ; le délai iOS évite qu'UIKit ignore un 2e dialogue présenté pendant la fermeture du 1er. |
| Police Android sur émulateur sans Google Play Services : repli sur Roboto | La police Plus Jakarta Sans passe par Google Fonts téléchargeable (`font_certs.xml`) : à vérifier sur appareil réel. |

## 8. CI/CD

**CI (tests uniquement)** : `.github/workflows/ci.yml`, workflow « CI ». **Aucun CD** (pas de build de release, de signature, de secret ni de déploiement : intervention séparée à venir). Détails, versions vérifiées et procédure de branch protection : `rapport-ci-setup.md`.
- **Déclencheurs** : `push` sur `dev` et `pull_request` ciblant `dev`. Sur une PR, une nouvelle poussée annule le run précédent (`concurrency`) ; sur `dev`, chaque commit garde son run.
- **Deux jobs INDÉPENDANTS** (pas de `needs`) — noms exacts = noms des *status checks* à rendre obligatoires : **`Android (JVM + JUnit)`** et **`iOS (natif + XCTest)`**.
  - `android` (`ubuntu-24.04`, 30 min) : `./gradlew :sharedLogic:testAndroidHostTest :androidApp:testDebugUnitTest --continue`. **Pas `allUnitTests`** : elle inclut `:sharedLogic:iosSimulatorArm64Test`, qui n'existe pas hors macOS. Artefact `android-test-reports`.
  - `ios` (`macos-26` arm64, 45 min) : `:sharedLogic:iosSimulatorArm64Test` puis `xcodebuild test -scheme iosAppTests` (simulateur = iPhone du runtime iOS le plus récent de l'image, sélectionné par script ; `-clonedSourcePackagesDirPath` pour cacher les paquets Swift ; `CODE_SIGNING_ALLOWED=NO`). Artefact `ios-test-reports` (rapports Gradle + `.xcresult` + journal xcodebuild).
- **Versions** (vérifiées le 2026-09-21) : JDK 17 Temurin (`setup-java`) — AGP 9.1.1 exige ≥ 17 et le projet est testé en 17 ; runner `macos-26` = macOS 26.6.2, **Xcode 26.6 par défaut**, simulateurs iOS 26.2/26.4/26.5 ; Ubuntu 24.04 avec Android SDK Platform `android-37.0` (compileSdk 37) ; actions `checkout@v7`, `setup-java@v6`, `gradle/actions/setup-gradle@v6`, `cache@v6`, `upload-artifact@v7`.
- **Caches** : Gradle via `setup-gradle` (**écrit seulement depuis `dev`**, lu par les PR : `cache-read-only` selon la branche) ; `~/.konan` (Kotlin/Native) et paquets Swift via `actions/cache` restore/save (sauvegarde en `always()` : un test en échec ne perd pas le téléchargement). Clés : hash de `gradle/libs.versions.toml` / de `Package.resolved`.
- **Pièges** : `hashFiles('gradle/libs.versions.toml')` change à chaque montée de version (Kotlin inclus) → cache konan re-téléchargé (voulu). Le premier run iOS sans cache est le plus long (durée à mesurer ; si > 45 min, relever `timeout-minutes`). Les rapports d'échec sont dans les artefacts, pas dans le journal.
- **Réglage manuel obligatoire (non automatisable)** : Settings → Branches → règle de protection (ou Ruleset) sur `dev` : PR obligatoire + les deux checks ci-dessus obligatoires. Sans lui la CI signale mais ne bloque rien.
- **Reproduire en local** : `./gradlew :sharedLogic:testAndroidHostTest :androidApp:testDebugUnitTest --continue` (Android), `./gradlew :sharedLogic:iosSimulatorArm64Test` puis `xcodebuild test …` (iOS), voir §5 bis. `actionlint .github/workflows/ci.yml` valide la syntaxe (installé via Homebrew) ; `act` ne sait pas exécuter de job macOS.

## 9. Identité de l'app : JobLog (nom, identifiants, logo)

- **Nom** : **JobLog** (ancien nom retiré partout, renommage complet fait avant toute publication). Affiché : label Android (`app_name`), `CFBundleDisplayName` iOS (`INFOPLIST_KEY_CFBundleDisplayName`), splash, « À propos » (`AboutContent.APP_NAME`), objet du mail de contact (« JobLog - Contact »). `PRODUCT_NAME = JobLog` (`Config.xcconfig`) → `JobLog.app` et **module Swift `JobLog`** (`@testable import JobLog`).
- **Identifiants** : package Kotlin **`com.dmb.joblog`** (dossiers `com/dmb/joblog/` dans tous les jeux de sources, y compris `sharedLogic/schemas/com.dmb.joblog.data.local.AppDatabase`), `applicationId`/`namespace` Android `com.dmb.joblog` (+ sous-paquets `sharedLogic` ; `sharedUI` suit le même préfixe), bundle iOS **`com.dmb.joblog`** (+ `$(TEAM_ID)` du `.xcconfig`, vide), tests XCTest `com.dmb.joblog.iosAppTests`. Renommage fait par déplacement de dossiers (`git mv`) + remplacement exact des anciens jetons (vérifié par compilation et par tous les tests) ; plus aucune occurrence de l'ancien nom dans le code ni ici (restent seulement le dossier local et le dépôt GitHub distant, non renommés). `rootProject.name = "JobLog"`.
- **NON renommés (décision)** : modules Gradle `sharedLogic` / `androidApp` / `sharedUI`, framework `SharedLogic`, cible Xcode `iosApp`, dossier local du projet, dépôt GitHub. **Base Room : `job_offers.db`** n'a JAMAIS contenu le nom de l'app → **aucun renommage de fichier ni migration nécessaire**. ⚠️ Changer d'`applicationId` / de bundle ID crée une **nouvelle app** pour le système : les données de l'ancienne installation ne sont **pas** reprises (impossible entre deux identifiants) ; acceptable car l'app n'était publiée nulle part. Désinstaller l'ancienne app des appareils/émulateurs de test.
- **Piège IDE après le renommage** : si Android Studio affiche « Activity class {<ancien package>/com.dmb.joblog.MainActivity} does not exist » (ancien package + nouvelle activité mélangés), c'est l'état local `.idea` (non versionné) qui date d'avant le renommage : faire *Sync Project with Gradle Files*, supprimer/recréer la configuration d'exécution `androidApp` (elle pointe sur le module au nom de l'ancien projet) et, en dernier recours, supprimer `.idea`. Le projet lui-même est cohérent (`applicationId`/`namespace` = `com.dmb.joblog`, manifeste fusionné et APK sans trace de l'ancien package ; voir `rapport-fix-activity-not-found.md`).
- **Logo** : **carnet / journal minimaliste + coche corail** (le concept « mallette » a été abandonné : lecture ambiguë « voyage »). Couverture blanche pleine 36 × 46 (x 36-72, y 31-77, rayon 5) centrée sur la grille 108 (diagonale 58,4 < cercle de sécurité 66), trait de reliure vertical (x 44,5, largeur 2,5, couleur du fond = « fente »), coche corail `#E8734A` (largeur 5,5) sur la couverture ; fond teal `#0D6E68`. **Source de vérité : `branding/generate_brand_assets.py`** (constantes `BODY_*`, `SPINE_*`, `CHECK_*`) qui régénère : masters SVG (`branding/joblog-logo.svg`, `joblog-mark-on-light.svg`), Android (`drawable/ic_launcher_background.xml`, `drawable-v24/ic_launcher_foreground.xml`, `drawable/ic_launcher_monochrome.xml` pour les icônes thématiques Android 13+ — couverture en contour, `drawable/ic_splash_logo.xml`, `drawable/ic_joblog_mark.xml`, `mipmap-anydpi-v26/ic_launcher*.xml`, PNG `mipmap-*/ic_launcher*.png` pour API 24-25) et iOS (`AppIcon.appiconset` : 1024 px clair, sombre, teinté, en RGB sans transparence). Dépendance : Pillow (PNG seulement). ⚠️ **Synchronisation manuelle** : `iosApp/iosApp/Core/Branding/JobLogMark.swift` (`JobLogMark`, `JobLogBrandTile`) redessine la même géométrie en SwiftUI avec les constantes recopiées à la main (fenêtre 36 × 46 d'origine 36,31 ; couverture rayon 5 ; reliure x 44,5 / 2,5 ; coche 51,54 → 57,60 → 66,47 / 5,5) : toute modification du script doit être reportée dans ce fichier (et inversement), puis vérifiée à l'écran (splash + pastille À propos). Emplacements : icône launcher Android/iOS, splash Android (`ic_splash_logo`) et iOS (`SplashView`, `JobLogMark` de 100 pt de large), pastille À propos (Android `ic_joblog_mark` sur `Teal40`, iOS `JobLogBrandTile`). Onboarding iOS : la **page 1** utilise `JobLogMark` (64 pt de haut, `bodyColor: .tealOnContainer`, `spineColor: .tealContainer`, dans le cercle `tealContainer`) ; les pages 2 et 3 gardent leurs icônes thématiques (`flag.fill`, `chart.bar.fill`). ⚠️ Android : la page 1 de l'onboarding utilise encore `Icons.Default.Work` (mallette) — écart iOS/Android connu, voir `rapport-fix-logo-onboarding.md`.

## 10. Internationalisation : anglais par défaut, français si la langue du système est le français

- **Comportement** : l'app démarre en **anglais** ; si la langue du système est le **français** (fr, fr-CA…), elle s'affiche en français ; **toute autre langue → anglais** (aucune 3e langue). Android : `values/strings.xml` (EN, défaut) + `values-fr/strings.xml`, `android:localeConfig` (`res/xml/locales_config.xml`, langue par app Android 13+), `androidResources.localeFilters = ["en","fr"]` (les ressources de bibliothèques dans d'autres langues sont retirées : pas de mélange). iOS : `iosApp/iosApp/en.lproj/Localizable.strings` (langue de développement) + `fr.lproj/…`, `knownRegions` `en, fr, Base`, `Info.plist` : `CFBundleDevelopmentRegion=en`, `CFBundleLocalizations=[en, fr]`.
- **Architecture retenue (à ne pas refaire par erreur)** : **deux mécanismes complémentaires, sans bibliothèque d'i18n.** (1) Les **libellés statiques d'écran** (liste, formulaire, statuts, tri, cartes) sont dans les **ressources natives** de chaque plateforme, avec **les mêmes clés** (`list_title`, `status_pending`, `form_field_title`…) ; le test **`LocalizationParityTest`** (JUnit Android, lit aussi les fichiers iOS) impose que les textes **partagés par les deux plateformes soient identiques** dans chaque langue (clés propres à une plateforme listées explicitement). (2) Le **contenu et les règles partagés** (onboarding, « À propos », message de validation du salaire, date courte `ShortDate`) restent dans `sharedLogic` — source unique — paramétrés par **`AppLanguage`** (`EN`/`FR`, `fromTag(tag)`, défaut `EN`) : `OnboardingContent.of(lang)`, `AboutContent.of(lang)`, `JobOfferFormLogic.validate(…, language)`, `ShortDate.format(date, lang)`. Chaque texte est écrit avec ses **deux traductions côte à côte** (`lang.pick(en = …, fr = …)`) : impossible d'oublier une langue. Chaque plateforme détecte la langue avec son mécanisme standard : Android `LocalConfiguration.locales[0]` (`rememberAppLanguage()`), iOS `Bundle.main.preferredLocalizations.first` (`AppLanguage.current` : la localisation que le système choisit parmi celles de l'app, donc espagnol → anglais).
- **Pourquoi pas une bibliothèque KMP** : les écrans sont natifs (Compose / SwiftUI), donc les bibliothèques de ressources partagées (Compose Multiplatform Resources, moko-resources…) ne sont utilisables que depuis du code Compose Multiplatform — pas depuis SwiftUI ni depuis `sharedLogic` — et imposeraient de déplacer les libellés hors des mécanismes natifs demandés (`strings.xml`, `Localizable.strings`). Le type maison est 1 enum + 1 fonction, testé, sans dépendance. (Comparaison en ligne non effectuée bibliothèque par bibliothèque ; la décision repose sur cette contrainte d'architecture.)
- **Détails de décision** : messages d'exception des use cases (`require`) et `JobOfferListState.errorMessage` : **anglais technique**, aucun écran ne les affiche (à traduire par code d'erreur si un écran les affichait un jour) ; `AboutState.errorMessage` remplacé par **`deletionFailed: Boolean`** (l'écran affiche `AboutContent.deleteAllFailedMessage` traduit) ; pluriels : clés `_one`/`_other` choisies par `count > 1` (identique sur les deux plateformes, pas de `plurals`/`stringsdict`) ; libellé de statut cité dans le texte de l'onboarding = libellé des écrans (test) ; titre du formulaire iOS unifié avec Android (« Edit application » / « Modifier la candidature » ; iOS affichait « Modifier ») ; le `prompt` de `.searchable` iOS est maintenant une clé de traduction (seul changement touchant `.searchable`, indispensable ; comportement, placement, décision inchangés) ; tri : libellés « Newest first / Oldest first » (EN), « Plus récent / Plus ancien » (FR).
- **Formats** : date courte de la carte : FR « 5 sept. », EN « Sep 5 » (`ShortDate`, identique Android/iOS) ; le sélecteur de date Android suit la langue du système (limité à en/fr par `localeFilters`).
- **Ajouter une langue** : ajouter une valeur à `AppLanguage`, une traduction à chaque `pick(...)` (le compilateur ne l'impose pas : les tests `everyText_isTranslated…` comparent EN/FR ; étendre), un `values-xx/strings.xml`, un `xx.lproj/Localizable.strings`, `locales_config.xml`, `localeFilters`, `knownRegions`, `CFBundleLocalizations`.
- **Tests** : `AppLanguageTest` (+ `ShortDate`), `OnboardingContentTest`, `AboutContentTest` (les deux langues, structure identique, aucun texte identique EN/FR, aucun accent français dans la version anglaise), `JobOfferFormLogicTest` (message par langue) en `commonTest` ; Android `LocalizationCompletenessTest` (mêmes clés EN/FR, mêmes arguments, pas d'accent dans `values/`), `LocalizationParityTest`, `NoHardCodedTextTest` (aucun littéral français dans le code Android), `AppLanguageAndroidTest` (Locale → langue), `StatusLabelsTest`/`SortOptionTest`/`DateFormattingTest` ; iOS `LocalizationTests` (mêmes clés, arguments, accents, résolution `en`/`fr`/autre, aucun littéral français dans les sources Swift), `CardDateFormatTests`, `StatusDisplayTests`. Vérifié à l'écran : Android (EN, FR, ES → EN) et iOS (EN, FR, ES → EN).
