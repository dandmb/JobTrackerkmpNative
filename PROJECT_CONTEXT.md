# JobTracker — contexte projet

> Ce fichier reflète l'état **actuel** du projet (pas son historique). À mettre à jour à chaque
> intervention : ajouter ce qui change, retirer ce qui n'est plus vrai.
> Dernière mise à jour : 2026-09-20 (parité visuelle iOS/Android de l'écran de liste).

## 1. Architecture générale

Application de suivi de candidatures. **Kotlin Multiplatform + MVVM + Room (KMP) + Koin**, avec des
**UI 100 % natives** : Jetpack Compose sur Android, SwiftUI sur iOS. Les deux UI consomment le même
ViewModel partagé.

| Module | Rôle |
|---|---|
| `sharedLogic` | Logique partagée (KMP : `androidMain`, `commonMain`, `iosMain`). Domaine, use cases, repository, Room, Koin, `JobOfferListViewModel`. Compilé en framework iOS statique `SharedLogic` (`iosArm64` + `iosSimulatorArm64`). |
| `androidApp` | App Android : Compose + Material3, thème, écrans (`ui/joboffer`, `ui/theme`, `ui/util`). |
| `iosApp` | App iOS : SwiftUI (`iosApp/iosApp/Core`, `Features/JobOffer`, `Resources/Fonts`). Xcode lance `./gradlew :sharedLogic:embedAndSignAppleFrameworkForXcode` avant de compiler. Groupes Xcode « synchronisés » : tout fichier ajouté dans `iosApp/iosApp/` est inclus automatiquement dans la cible. |
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
| kotlinx-coroutines | 1.10.1 |
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

Statuts (identiques clair/sombre, **non adaptatifs**, sur les deux plateformes) :
PENDING `#79747E`, APPLIED `#0D6E68` (= Teal40), INTERVIEW `#E8734A` (= Coral40), REJECTED `#BA1A1A`, ACCEPTED `#2E7D32`.

Où c'est défini : Android → `ui/theme/Color.kt`, `Theme.kt`, `StatusColors.kt` ;
iOS → `iosApp/Core/Theme/Color+Theme.swift` (couleurs adaptatives clair/sombre via `Color(light:dark:)`),
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
  chip de statut cliquable (hauteur 32, coin 8, contour outlineVariant 1, label coloré par statut, flèche « dropdown » 18) ; divider ; dates
  Postulé / Entretien / Résultat (label MAJUSCULES 11 medium, tracking 0.5, onSurfaceVariant ; valeur labelLarge SemiBold, format `d MMM` en **anglais**) ; salaire `💰 …`.
- Carte stats : fond primaryContainer, coin 12, padding 20 ; total 34 Bold (interligne 30) ; « candidature(s) suivie(s) » à 80 % ; badges par statut fond `couleur @ 16 %`, coin 10, padding 10×6.
- Logique de casse : Android `ui/util/TextCase.kt` ↔ iOS `Core/Extensions/String+Case.swift`.

## 5. État des lieux fonctionnel

Fait :
- CRUD des candidatures (ajout, édition via feuille de formulaire, suppression, changement de statut depuis la carte) — Android et iOS.
- Recherche (titre/entreprise, insensible à la casse, faite côté UI, les stats portent sur **toutes** les offres).
- Tri : plus récent / plus ancien / A→Z / Z→A.
- Carte de statistiques (total + compteurs par statut).
- Suppression : swipe (Android : `SwipeToDismissBox` + snackbar ; iOS : `swipeActions`).
- Persistance Room avec migration 1→2.

Constats / reste à faire (observés dans le code, pas de roadmap officielle) :
- `JobOfferListEvent` est une classe vide ; `sharedUI` est un template non utilisé.
- Aucun test dans le dépôt (les dépendances `kotlin-test` sont déclarées).
- Pas d'annulation de suppression sur iOS (Android affiche seulement un snackbar informatif).

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
| Format de date `d MMM` en **anglais** (« 20 Sep ») sur les deux | Reproduit tel quel depuis Android (`MonthNames.ENGLISH_ABBREVIATED`) ; à passer en français des deux côtés si voulu. |
| Couleurs de statut non adaptatives : faible contraste en dark mode (Applied teal / Rejected rouge sur fond sombre) | Fidèle à Android (`Color.kt`) ; à corriger des deux côtés simultanément. |
| Title case : « iOS engineer » → « IOS Engineer » | Logique `TextCase.kt` reproduite fidèlement (ne touche que la 1re lettre si minuscule). |
