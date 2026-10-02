# Aucune règle propre au projet, volontairement : les règles nécessaires sont livrées par les bibliothèques
# (Room, sqlite-bundled, AndroidX Startup, Koin, kotlinx-datetime) et le code de l'app n'utilise pas la réflexion.
# Avant d'ajouter une dépendance qui instancie des classes par leur nom ou sérialise par réflexion, rejouer les
# parcours sur le build release minifié : debug et staging ne passent pas par R8 et ne révèlent pas ces pannes.
