package br.com.devcapu.remy.recipe

import java.util.UUID

data class Recipe(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val ingredients: List<Ingredient>,
    val steps: List<String>,
    val totalTime: String? = null,
    val servings: Int? = null,
)

data class Ingredient(
    val name: String,
    val quantity: String,
    val unit: String? = null
)

// SEGUNDA-FEIRA - Virado à Paulista
private val viradoPaulistaRecipe = Recipe(
    name = "Virado à Paulista",
    ingredients = listOf(
        // Arroz
        Ingredient(name = "Arroz", quantity = "2", unit = "xícaras"),
        Ingredient(name = "Água", quantity = "4", unit = "xícaras"),
        Ingredient(name = "Óleo", quantity = "2", unit = "colheres de sopa"),
        Ingredient(name = "Alho", quantity = "2", unit = "dentes"),
        Ingredient(name = "Cebola", quantity = "1/2", unit = "unidade"),

        // Tutu de feijão
        Ingredient(name = "Feijão carioca", quantity = "500", unit = "g"),
        Ingredient(name = "Farinha de mandioca", quantity = "200", unit = "g"),
        Ingredient(name = "Bacon", quantity = "100", unit = "g"),

        // Carnes
        Ingredient(name = "Bisteca de porco", quantity = "4", unit = "unidades"),
        Ingredient(name = "Linguiça calabresa", quantity = "300", unit = "g"),
        Ingredient(name = "Torresmo", quantity = "200", unit = "g"),

        // Outros acompanhamentos
        Ingredient(name = "Ovos", quantity = "4", unit = "unidades"),
        Ingredient(name = "Bananas nanicas", quantity = "4", unit = "unidades"),
        Ingredient(name = "Farinha de rosca", quantity = "100", unit = "g"),
        Ingredient(name = "Óleo para fritar", quantity = "500", unit = "ml"),
        Ingredient(name = "Sal", quantity = "a gosto", unit = null),
        Ingredient(name = "Pimenta-do-reino", quantity = "a gosto", unit = null)
    ),
    steps = listOf(
        "Cozinhe o feijão até ficar bem macio. Reserve o caldo.",
        "Frite o bacon em cubinhos até dourar. Reserve a gordura.",
        "Na gordura do bacon, refogue cebola e alho, adicione o feijão e vá incorporando a farinha de mandioca aos poucos.",
        "Prepare o arroz branco tradicional.",
        "Tempere as bistecas com sal e pimenta, grelhe até dourar.",
        "Frite a linguiça em rodelas até dourar.",
        "Prepare o torresmo fritando a pele de porco até ficar crocante.",
        "Frite os ovos mantendo as gemas moles.",
        "Passe as bananas na farinha de rosca e frite até dourar.",
        "Sirva todos os componentes no mesmo prato."
    ),
    totalTime = "2h",
    servings = 4
)

// TERÇA-FEIRA - Bife à Rolê
private val bifeRoleRecipe = Recipe(
    name = "Bife à Rolê",
    ingredients = listOf(
        // Carne
        Ingredient(name = "Coxão mole", quantity = "800", unit = "g"),
        Ingredient(name = "Sal", quantity = "a gosto", unit = null),
        Ingredient(name = "Pimenta-do-reino", quantity = "a gosto", unit = null),

        // Recheio
        Ingredient(name = "Cenoura", quantity = "2", unit = "unidades"),
        Ingredient(name = "Vagem", quantity = "200", unit = "g"),
        Ingredient(name = "Azeitonas verdes", quantity = "100", unit = "g"),
        Ingredient(name = "Linguiça calabresa", quantity = "200", unit = "g"),
        Ingredient(name = "Bacon", quantity = "100", unit = "g"),
        Ingredient(name = "Cebola", quantity = "1", unit = "unidade"),
        Ingredient(name = "Alho", quantity = "3", unit = "dentes"),

        // Acompanhamentos
        Ingredient(name = "Arroz", quantity = "2", unit = "xícaras"),
        Ingredient(name = "Batatas", quantity = "6", unit = "unidades"),
        Ingredient(name = "Leite", quantity = "200", unit = "ml"),
        Ingredient(name = "Manteiga", quantity = "3", unit = "colheres de sopa"),

        // Para cozinhar
        Ingredient(name = "Óleo", quantity = "4", unit = "colheres de sopa"),
        Ingredient(name = "Caldo de carne", quantity = "500", unit = "ml"),
        Ingredient(name = "Barbante culinário", quantity = "1", unit = "rolo")
    ),
    steps = listOf(
        "Corte a carne em fatias finas e bata para amaciar. Tempere com sal e pimenta.",
        "Corte cenoura, vagem, linguiça e bacon em tiras. Pique cebola e alho.",
        "Refogue os legumes e carnes do recheio até ficarem al dente.",
        "Coloque o recheio sobre cada fatia de carne, enrole e amarre com barbante.",
        "Doure os rolinhos em panela com óleo quente.",
        "Adicione caldo de carne, tampe e cozinhe por 45 minutos em fogo baixo.",
        "Prepare o arroz branco tradicional.",
        "Cozinhe as batatas, escorra e faça purê com leite e manteiga.",
        "Sirva os bife à rolê com o molho do cozimento, arroz e purê."
    ),
    totalTime = "1h30min",
    servings = 4
)

// QUARTA-FEIRA - Feijoada
private val feijoada = Recipe(
    name = "Feijoada Completa",
    ingredients = listOf(
        // Feijão
        Ingredient(name = "Feijão preto", quantity = "500", unit = "g"),

        // Carnes
        Ingredient(name = "Costela de porco", quantity = "300", unit = "g"),
        Ingredient(name = "Linguiça calabresa", quantity = "200", unit = "g"),
        Ingredient(name = "Linguiça paio", quantity = "200", unit = "g"),
        Ingredient(name = "Bacon", quantity = "200", unit = "g"),
        Ingredient(name = "Carne seca", quantity = "200", unit = "g"),
        Ingredient(name = "Orelha de porco", quantity = "1", unit = "unidade"),
        Ingredient(name = "Rabo de porco", quantity = "1", unit = "unidade"),

        // Temperos
        Ingredient(name = "Cebola", quantity = "2", unit = "unidades"),
        Ingredient(name = "Alho", quantity = "6", unit = "dentes"),
        Ingredient(name = "Louro", quantity = "3", unit = "folhas"),
        Ingredient(name = "Óleo", quantity = "3", unit = "colheres de sopa"),

        // Acompanhamentos
        Ingredient(name = "Arroz", quantity = "2", unit = "xícaras"),
        Ingredient(name = "Couve-manteiga", quantity = "1", unit = "maço"),
        Ingredient(name = "Farinha de mandioca", quantity = "2", unit = "xícaras"),
        Ingredient(name = "Laranja", quantity = "2", unit = "unidades"),
        Ingredient(name = "Pimenta dedo-de-moça", quantity = "2", unit = "unidades")
    ),
    steps = listOf(
        "Deixe o feijão preto de molho na véspera.",
        "Dessalgue a carne seca deixando de molho por 2 horas.",
        "Cozinhe o feijão em panela de pressão até ficar macio.",
        "Em panela grande, doure todas as carnes aos poucos.",
        "Refogue cebola e alho, adicione as carnes douradas.",
        "Junte o feijão cozido com o caldo, adicione folhas de louro.",
        "Cozinhe em fogo baixo por 1 hora, mexendo ocasionalmente.",
        "Prepare arroz branco, couve refogada em fatias finas.",
        "Faça farofa dourada com a farinha de mandioca.",
        "Sirva com rodelas de laranja e pimenta."
    ),
    totalTime = "3h",
    servings = 6
)

// QUINTA-FEIRA - Macarrão
private val macarraoBolonesaRecipe = Recipe(
    name = "Espaguete à Bolonhesa",
    ingredients = listOf(
        // Massa
        Ingredient(name = "Espaguete", quantity = "500", unit = "g"),
        Ingredient(name = "Sal", quantity = "1", unit = "colher de sopa"),
        Ingredient(name = "Óleo", quantity = "1", unit = "colher de sopa"),

        // Molho
        Ingredient(name = "Carne moída", quantity = "500", unit = "g"),
        Ingredient(name = "Tomates", quantity = "6", unit = "unidades"),
        Ingredient(name = "Cebola", quantity = "1", unit = "unidade"),
        Ingredient(name = "Alho", quantity = "4", unit = "dentes"),
        Ingredient(name = "Cenoura", quantity = "1", unit = "unidade"),
        Ingredient(name = "Aipo", quantity = "2", unit = "talos"),
        Ingredient(name = "Vinho tinto", quantity = "100", unit = "ml"),
        Ingredient(name = "Extrato de tomate", quantity = "2", unit = "colheres de sopa"),
        Ingredient(name = "Manjericão", quantity = "1", unit = "maço"),
        Ingredient(name = "Orégano", quantity = "1", unit = "colher de chá"),
        Ingredient(name = "Azeite", quantity = "4", unit = "colheres de sopa"),
        Ingredient(name = "Queijo parmesão", quantity = "100", unit = "g"),
        Ingredient(name = "Pimenta-do-reino", quantity = "a gosto", unit = null)
    ),
    steps = listOf(
        "Pique finamente cebola, alho, cenoura e aipo (refogado).",
        "Escalde os tomates, retire a pele e pique grosseiramente.",
        "Aqueça o azeite e refogue os legumes do refogado até ficarem macios.",
        "Adicione a carne moída e doure bem, quebrando os torrões.",
        "Junte o vinho e deixe evaporar o álcool.",
        "Acrescente os tomates, extrato de tomate e temperos.",
        "Cozinhe em fogo baixo por 45 minutos, mexendo ocasionalmente.",
        "Cozinhe o espaguete em água fervente com sal até ficar al dente.",
        "Escorra a massa e misture com um pouco do molho.",
        "Sirva o espaguete coberto com o molho e queijo ralado."
    ),
    totalTime = "1h15min",
    servings = 4
)

// SEXTA-FEIRA - Pescados
private val pescadaAssadaRecipe = Recipe(
    name = "Pescada Assada com Legumes",
    ingredients = listOf(
        // Peixe
        Ingredient(name = "Pescada limpa", quantity = "1", unit = "kg"),
        Ingredient(name = "Limão", quantity = "2", unit = "unidades"),
        Ingredient(name = "Sal", quantity = "a gosto", unit = null),
        Ingredient(name = "Pimenta-do-reino", quantity = "a gosto", unit = null),
        Ingredient(name = "Alho", quantity = "4", unit = "dentes"),

        // Legumes
        Ingredient(name = "Batatas", quantity = "4", unit = "unidades"),
        Ingredient(name = "Cebolas", quantity = "2", unit = "unidades"),
        Ingredient(name = "Tomates", quantity = "3", unit = "unidades"),
        Ingredient(name = "Pimentão", quantity = "1", unit = "unidade"),
        Ingredient(name = "Abobrinha", quantity = "1", unit = "unidade"),

        // Temperos e molho
        Ingredient(name = "Azeite", quantity = "6", unit = "colheres de sopa"),
        Ingredient(name = "Vinho branco", quantity = "100", unit = "ml"),
        Ingredient(name = "Salsinha", quantity = "3", unit = "colheres de sopa"),
        Ingredient(name = "Cebolinha", quantity = "2", unit = "colheres de sopa"),
        Ingredient(name = "Louro", quantity = "2", unit = "folhas"),

        // Acompanhamento
        Ingredient(name = "Arroz", quantity = "2", unit = "xícaras")
    ),
    steps = listOf(
        "Limpe bem a pescada e faça cortes superficiais nas laterais.",
        "Tempere com sal, pimenta, alho amassado e suco de limão. Deixe marinar por 30 minutos.",
        "Corte todos os legumes em fatias médias.",
        "Preaqueça o forno a 200°C.",
        "Em uma assadeira, disponha metade dos legumes temperados com azeite e sal.",
        "Coloque a pescada por cima e cubra com o restante dos legumes.",
        "Regue com azeite, vinho branco e coloque as folhas de louro.",
        "Asse por 35-40 minutos ou até o peixe estar bem cozido.",
        "Prepare o arroz branco para acompanhar.",
        "Finalize com salsinha e cebolinha picadas antes de servir."
    ),
    totalTime = "1h30min",
    servings = 4
)

val allRecipes = listOf(
    viradoPaulistaRecipe,
    bifeRoleRecipe,
    feijoada,
    macarraoBolonesaRecipe,
    pescadaAssadaRecipe
)
