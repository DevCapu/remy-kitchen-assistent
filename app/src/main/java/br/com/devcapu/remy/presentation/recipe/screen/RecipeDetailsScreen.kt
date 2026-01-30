package br.com.devcapu.remy.presentation.recipe.screen

import ai.picovoice.porcupine.PorcupineManager
import android.speech.tts.TextToSpeech
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.HeadsetOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import br.com.devcapu.remy.R
import br.com.devcapu.remy.conversation.VoiceRecognitionService
import br.com.devcapu.remy.infra.PorcupineManagerSingleton
import br.com.devcapu.remy.data.recipe.Recipe

@Composable
fun RecipeDetailsScreen(
    modifier: Modifier = Modifier,
    recipe: Recipe,
    isChefMode: Boolean = false,
    canSpeak: Boolean = false,
    onSpeak: (String, Int) -> Unit = { _, _ -> },
    onStopSpeaking: () -> Unit = {}
) {
    val context = LocalContext.current
    var isVoiceAssistantEnabled by remember { mutableStateOf(false) }

    val ingredients = recipe.ingredients
    var selectedStepIndex by remember { mutableIntStateOf(0) }

    val currentStepIngredients by remember(selectedStepIndex, isChefMode) {
        derivedStateOf {
            if (isChefMode) {
                val currentStep = recipe.steps[selectedStepIndex]
                ingredients.filter { currentStep.contains(it.name, true) }
            } else {
                ingredients
            }
        }
    }


    val speakCurrentStep = {
        if (canSpeak && isVoiceAssistantEnabled) {
            val stepText =
                "Passo ${selectedStepIndex + 1} de ${recipe.steps.size}: ${recipe.steps[selectedStepIndex]}"
            val ingredientsText = if (currentStepIngredients.isNotEmpty()) {
                "Ingredientes necessários: " + currentStepIngredients.joinToString(", ") {
                    "${it.name}, ${it.quantity} ${it.unit.orEmpty()}"
                }
            } else ""

            onSpeak("$stepText. $ingredientsText", TextToSpeech.QUEUE_FLUSH)
        }
    }

    var porcupineManager by remember { mutableStateOf<PorcupineManager?>(null) }
    var porcupineIsListening by remember { mutableStateOf(false) }

    val voiceRecognitionCallback = object : VoiceRecognitionService.VoiceRecognitionCallback {
        override fun onCommandRecognized(command: VoiceRecognitionService.VoiceCommand) {
            when (command) {
                VoiceRecognitionService.VoiceCommand.NEXT_STEP -> {
                    if (selectedStepIndex < recipe.steps.size - 1) {
                        selectedStepIndex++
                    }
                }

                VoiceRecognitionService.VoiceCommand.PREVIOUS_STEP -> {
                    if (selectedStepIndex > 0) {
                        selectedStepIndex--
                    }
                }

                VoiceRecognitionService.VoiceCommand.REPEAT_STEP -> {
                    speakCurrentStep()
                }

                VoiceRecognitionService.VoiceCommand.INGREDIENTS -> {
                    if (currentStepIngredients.isNotEmpty()) {
                        val ingredientsText =
                            "Ingredientes necessários: " + currentStepIngredients.joinToString(
                                ", "
                            ) {
                                "${it.name}, ${it.quantity} ${it.unit.orEmpty()}"
                            }
                        onSpeak(
                            ingredientsText,
                            TextToSpeech.QUEUE_ADD
                        )
                    } else {
                        onSpeak(
                            "Nenhum ingrediente necessário para este passo.",
                            TextToSpeech.QUEUE_ADD
                        )
                    }
                }

                else -> {
                    onSpeak(
                        "Comando não reconhecido.",
                        TextToSpeech.QUEUE_ADD
                    )
                }
            }
            porcupineManager?.start()
            porcupineIsListening = true
        }

        override fun onTextRecognized(text: String) = Unit

        override fun onError(error: String) {
            porcupineManager?.start()
            porcupineIsListening = true
        }

        override fun onListeningStateChanged(isListening: Boolean) = Unit
    }
    val voiceRecognitionService = VoiceRecognitionService(context, voiceRecognitionCallback)
    porcupineManager = PorcupineManagerSingleton.getInstance(context, voiceRecognitionService)

    LaunchedEffect(selectedStepIndex, isVoiceAssistantEnabled) {
        if (isVoiceAssistantEnabled && isChefMode) {
            speakCurrentStep()
        }
    }


    val hasIngredients = currentStepIngredients.isNotEmpty()

    if (!isChefMode) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                RecipeItem(
                    modifier = Modifier
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                    recipe = recipe,
                    chefMode = isChefMode,
                    onClickItem = { }
                )
            }
            if (hasIngredients) {
                item {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .animateContentSize(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                stringResource(R.string.ingredients),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            currentStepIngredients.forEach { ingredient ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = ingredient.name,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = "${ingredient.quantity} ${ingredient.unit.orEmpty()}"
                                    )
                                }
                                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                            }
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.preparation_mode),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        recipe.steps.forEachIndexed { idx, step ->
                            Text(
                                style = MaterialTheme.typography.displaySmall,
                                text = "${idx + 1}. $step"
                            )
                        }
                    }
                }
            }
        }
    } else {
        ConstraintLayout(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            val (recipeRef, ingredientsRef, stepsRef, controlsRef) = createRefs()

            RecipeItem(
                modifier = Modifier.constrainAs(recipeRef) {
                    top.linkTo(parent.top, margin = 16.dp)
                    start.linkTo(parent.start, margin = 16.dp)
                    end.linkTo(parent.end, margin = 16.dp)
                },
                recipe = recipe,
                chefMode = true,
                onClickItem = { }
            )

            if (hasIngredients) {
                Card(
                    modifier = Modifier
                        .constrainAs(ingredientsRef) {
                            top.linkTo(recipeRef.bottom, margin = 8.dp)
                            start.linkTo(parent.start, margin = 16.dp)
                            end.linkTo(parent.end, margin = 16.dp)
                        }
                        .animateContentSize(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            stringResource(R.string.ingredients),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        currentStepIngredients.forEach { ingredient ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = ingredient.name,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "${ingredient.quantity} ${ingredient.unit.orEmpty()}"
                                )
                            }
                            HorizontalDivider(Modifier.padding(vertical = 6.dp))
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.constrainAs(stepsRef) {
                    top.linkTo(
                        if (hasIngredients) ingredientsRef.bottom else recipeRef.bottom,
                        margin = 8.dp
                    )
                    start.linkTo(parent.start, margin = 16.dp)
                    end.linkTo(parent.end, margin = 16.dp)
                    bottom.linkTo(controlsRef.top, margin = 16.dp)
                    height = Dimension.fillToConstraints
                }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 8.dp
                        ),
                        text = stringResource(
                            R.string.preparation_mode_step,
                            selectedStepIndex + 1,
                            recipe.steps.size
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )

                    CircularList(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp)
                            .fillMaxWidth(),
                        items = recipe.steps,
                        isEndless = false,
                        selectedStepIndex = selectedStepIndex
                    ) {
                        selectedStepIndex = it
                    }
                }
            }

            Row(
                modifier = Modifier
                    .constrainAs(controlsRef) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom, margin = 16.dp)
                    }
                    .fillMaxWidth()
                    .padding(end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingActionButton(
                    onClick = {
                        if (selectedStepIndex > 0) {
                            selectedStepIndex--
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.NavigateBefore,
                        contentDescription = stringResource(R.string.previous_step)
                    )
                }

                FloatingActionButton(onClick = {
                    isVoiceAssistantEnabled = !isVoiceAssistantEnabled
                    if (isVoiceAssistantEnabled) {
                        speakCurrentStep()
                    } else {
                        onStopSpeaking()
                    }
                }) {
                    Icon(
                        imageVector = if (isVoiceAssistantEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = if (isVoiceAssistantEnabled)
                            stringResource(R.string.disable_voice_assistant)
                        else
                            stringResource(R.string.enable_voice_assistant)
                    )
                }

                FloatingActionButton(
                    onClick = {
                        porcupineIsListening = !porcupineIsListening
                        if (porcupineIsListening) {
                            porcupineManager?.start()
                        } else {
                            porcupineManager?.stop()

                        }
                    }) {
                    Icon(
                        imageVector = if (porcupineIsListening) Icons.Default.Headset else Icons.Default.HeadsetOff,
                        contentDescription = if (isVoiceAssistantEnabled)
                            stringResource(R.string.disable_voice_assistant)
                        else
                            stringResource(R.string.enable_voice_assistant)
                    )
                }

                // Botão para avançar ao próximo passo
                FloatingActionButton(
                    onClick = {
                        if (selectedStepIndex < recipe.steps.size - 1) {
                            selectedStepIndex++
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.NavigateNext,
                        contentDescription = stringResource(R.string.next_step)
                    )
                }
            }
        }
    }
}

@Composable
fun CircularList(
    items: List<String>,
    modifier: Modifier = Modifier,
    isEndless: Boolean = false,
    selectedStepIndex: Int,
    onItemClick: (index: Int) -> Unit = { }
) {
    val listState = rememberLazyListState(
        if (isEndless) Int.MAX_VALUE / 2 else 0
    )

    LaunchedEffect(selectedStepIndex) {
        listState.animateScrollToItem(selectedStepIndex)
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            count = if (isEndless) Int.MAX_VALUE else items.size,
            itemContent = {
                val index = it % items.size
                val textModifier = if (index == selectedStepIndex) {
                    Modifier
                        .background(
                            color = Color(0x3B55FF00),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .fillMaxWidth()
                        .padding(8.dp)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                }
                Text(
                    modifier = textModifier.clickable { onItemClick(index) },
                    style = MaterialTheme.typography.headlineMedium,
                    text = items[index],
                    color = if (index == selectedStepIndex) Color.Black else Color.Black.copy(alpha = 0.5f)
                )
            }
        )
    }
}

@Preview(showSystemUi = true, showBackground = false, name = "Modo Chef")
@Composable
private fun ChefModeRecipeScreenPreview() {
    RecipeDetailsScreen(
        recipe = Recipe(
            name = "Preview",
            ingredients = emptyList(),
            steps = listOf("Passo 1", "Passo 2")
        ),
        isChefMode = true
    )
}

@Preview(showSystemUi = true, showBackground = false, name = "Visualização Normal")
@Composable
private fun RecipeScreenPreview() {
    RecipeDetailsScreen(
        recipe = Recipe(
            name = "Preview",
            ingredients = emptyList(),
            steps = listOf("Passo 1", "Passo 2")
        ),
        isChefMode = false
    )
}