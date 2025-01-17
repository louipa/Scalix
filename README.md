## Groupe 
- Antoine OTEGUI
- Louis PAINTER

Classe : FIL A2 2026

## Architecture Scalix

### Introduction
Scalix est une application en Scala permettant de rechercher des informations sur des acteur.ices, leurs films, et les collaborations via l'API de The Movie Database (TMDB). L'application utilise un système de mise en cache pour limiter les appels à l'API externe et améliorer les performances.

## Fonctionnalités

- Recherche d'acteur.ice par nom et prénom
- liste des films d'un acteur.ice
- Recherche du réalisateur.ice d'un film
- Recherche des films en collaboration avec deux acteur.ices

## Cache 
Chaque méthode peut faire appel au cache.
Le mécanisme du cache suit la spécification suivante :

- si la donnée demandée est présente dans le cache primaire (dictionnaire en mémoire), alors on la retourne.
- sinon, on vérifie dans le cache secondaire (fichiers JSON dans le dossier `cache`), et on la retourne si elle est présente. On met également le cache primaire à jour.
- enfin, si la donnée n'est pas présente dans le cache secondaire, on fait la requête à l'API, on met à jour le cache primaire et secondaire, et on retourne la donnée.

Pour cacher une fonction, il suffit de trouver une clé unique à la donnée demandée et appeler la fonction comme suit :

```scala
// Non caché
val result = requestHttp("my data")

// Caché
val key = ???
val result = useCache(key)(requestHttp("my data"))
```

## Utilisation

Ouvrir par exemple Scala REPL et utilisez les fonctions disponible dans le fichier `Scalix.scala` :

```scala
scala> Scalix.findActor("Tom", "Hanks")
val res0: Option[Int] = Some(31)

scala> Scalix.findActorMovies(31)
val res1: Set[(Int, String)] = ...

scala> Scalix.findMovieDirector(16279)
val res2: Option[(Int, String)] = Some((81199,Sean McGinly))

scala> Scalix.findCollaborations(("Tom", "Hanks"), ("Rita", "Wilson"))
val res3: Set[(String, String)] = ...

scala> Scalix.mostFrequentActorPairs(Seq(("Tom", "Hanks"), ("Rita", "Wilson"), ("Robin", "Williams")))
val res4: Seq[((String, String), (String, String), Int)] = ...
```

## Quiz 4.1

Par manque de temps, il était plus simple de faire une fonction qui prend une séquence d'acteur.ices plutôt que de les chercher dans le cache primaire.

