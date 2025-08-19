tasks.register<Copy>("copyTypeshed") {
    val hashFile = file(".git/modules/typeshed/refs/heads/main")
    if (hashFile.exists()) {
        val commitHash = hashFile.readText().trim()
        val targetFile = file("typeshed/odools_commit_hash.txt")
        targetFile.writeText(commitHash)
    }

    val typeshedDir = file("src/main/resources/typeshed")
    doFirst {
        if (typeshedDir.exists()) {
            typeshedDir.deleteRecursively()
        }
    }

    from("typeshed")
    into("src/main/resources/typeshed")
}