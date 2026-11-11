package com.zenkaigym.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zenkaigym.data.ExerciseImages
import com.zenkaigym.domain.tr
import com.zenkaigym.ui.theme.Zen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Ilustração do exercício com o crédito do autor logo abaixo; não ocupa espaço se não houver imagem. */
@Composable
fun ExerciseImage(exerciseName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, exerciseName) {
        value = withContext(Dispatchers.IO) { ExerciseImages.load(context, exerciseName)?.asImageBitmap() }
    }
    val image = bitmap ?: return
    val credit = ExerciseImages.creditFor(context, exerciseName)
    Column(modifier) {
        Image(
            bitmap = image,
            contentDescription = tr(exerciseName),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(12.dp))
        )
        if (credit != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                tr("Imagem: {0} · {1} · wger.de", credit.author, credit.license),
                color = Zen.p.muted, style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
