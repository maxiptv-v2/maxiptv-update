package com.maxiptv.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.maxiptv.MaxiApp

/**
 * Constraints inteligentes baseados no tipo de dispositivo
 * Previne layouts estourados antes de renderizar
 */
object SafeLayoutConstraints {
    /**
     * Retorna largura máxima segura para o dispositivo atual
     */
    @Composable
    fun getMaxWidth(): Dp {
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp.dp
        
        return when {
            MaxiApp.isFireStick -> screenWidthDp - 48.dp // Fire Stick: margem maior
            MaxiApp.isTvBox -> screenWidthDp - 40.dp     // TV Box: margem média
            MaxiApp.isNativeTv -> screenWidthDp - 32.dp // Native TV: margem menor
            else -> Dp.Unspecified                      // Smartphone: sem limite
        }
    }
    
    /**
     * Retorna padding horizontal seguro para o dispositivo atual
     */
    fun getHorizontalPadding(): Dp {
        return when {
            MaxiApp.isFireStick -> 24.dp
            MaxiApp.isTvBox -> 20.dp
            MaxiApp.isNativeTv -> 16.dp
            else -> 16.dp
        }
    }
    
    /**
     * Retorna número máximo de linhas para textos baseado no dispositivo
     */
    fun getTextMaxLines(): Int {
        return if (MaxiApp.isTv) 2 else 3 // TV: menos linhas para evitar overflow vertical
    }
}

/**
 * Modifier que garante que o elemento nunca saia da tela em TVs
 * Aplica automaticamente largura máxima e padding baseado no dispositivo
 */
@Composable
fun Modifier.safeForTv(): Modifier {
    val maxWidth = SafeLayoutConstraints.getMaxWidth()
    val padding = SafeLayoutConstraints.getHorizontalPadding()
    
    return if (MaxiApp.isTv && maxWidth != Dp.Unspecified) {
        this
            .widthIn(max = maxWidth)
            .padding(horizontal = padding)
    } else {
        this.padding(horizontal = padding)
    }
}

/**
 * Modifier que aplica fillMaxWidth com fator reduzido para Fire Stick e Native TV
 * Esses dispositivos mostram aproximadamente 90% da medida real da tela
 * TV Box genérica não precisa porque já funciona corretamente
 */
@Composable
fun Modifier.fillMaxWidthAdjusted(): Modifier {
    return when {
        MaxiApp.isFireStick -> {
            // Fire Stick: mostra ~90% da medida real, aplicar 0.90f
            this.fillMaxWidth(0.90f)
        }
        MaxiApp.isNativeTv -> {
            // Native TV: mostra ~90% da medida real, aplicar 0.90f
            this.fillMaxWidth(0.90f)
        }
        MaxiApp.isTvBox -> {
            // TV Box genérica: já funciona corretamente, usar 100%
            this.fillMaxWidth()
        }
        else -> {
            // Outros dispositivos: usar 100%
            this.fillMaxWidth()
        }
    }
}

/**
 * Modifier para textos que nunca estouram
 * Aplica largura máxima e garante ellipsis
 */
@Composable
fun Modifier.safeText(): Modifier {
    val maxWidth = SafeLayoutConstraints.getMaxWidth()
    
    return if (MaxiApp.isTv && maxWidth != Dp.Unspecified) {
        this.widthIn(max = maxWidth)
    } else {
        this
    }
}

/**
 * Componente de texto seguro que nunca estoura
 * Sempre aplica ellipsis e limita largura
 */
@Composable
fun SafeText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = SafeLayoutConstraints.getTextMaxLines(),
    fontSize: TextUnit = 14.sp,
    color: Color = Color.Unspecified,
    fontWeight: androidx.compose.ui.text.font.FontWeight? = null
) {
    Text(
        text = text,
        modifier = modifier.safeText(),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis, // ✅ Sempre corta se muito longo
        fontSize = fontSize,
        color = color,
        fontWeight = fontWeight
    )
}

/**
 * Componente de botão seguro que nunca estoura em TVs
 * Limita largura automaticamente baseado no dispositivo
 */
@Composable
fun SafeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val maxWidth = SafeLayoutConstraints.getMaxWidth()
    
    Button(
        onClick = onClick,
        modifier = modifier.then(
            if (MaxiApp.isTv && maxWidth != Dp.Unspecified) {
                Modifier.widthIn(max = maxWidth)
            } else {
                Modifier.fillMaxWidth()
            }
        ),
        enabled = enabled
    ) {
        content()
    }
}

/**
 * Row horizontal scrollável seguro que nunca estoura
 * Aplica padding automático e garante scroll funcional
 */
@Composable
fun SafeHorizontalScrollableRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = SafeLayoutConstraints.getHorizontalPadding()),
    content: @Composable RowScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(contentPadding), // ✅ Padding automático para evitar overflow
        horizontalArrangement = horizontalArrangement
    ) {
        content()
    }
}

/**
 * Column segura que garante padding adequado
 * Previne elementos filhos de saírem da tela
 */
@Composable
fun SafeColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: androidx.compose.ui.Alignment.Horizontal = androidx.compose.ui.Alignment.Start,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = SafeLayoutConstraints.getHorizontalPadding(),
        vertical = 16.dp
    ),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment
    ) {
        content()
    }
}

/**
 * Box segura que garante elementos não saiam da tela
 * Útil para containers que precisam de limites
 */
@Composable
fun SafeBox(
    modifier: Modifier = Modifier,
    contentAlignment: androidx.compose.ui.Alignment = androidx.compose.ui.Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    val maxWidth = SafeLayoutConstraints.getMaxWidth()
    val padding = SafeLayoutConstraints.getHorizontalPadding()
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (MaxiApp.isTv && maxWidth != Dp.Unspecified) {
                    Modifier.widthIn(max = maxWidth)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = padding),
        contentAlignment = contentAlignment
    ) {
        content()
    }
}

