package com.diu.yk_games.line2box.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.model.Banner
import com.diu.yk_games.line2box.util.bounceClick
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun BannerPager(
    banners: PersistentList<Banner>,
    onItemClick: (redirectUrl: String) -> Unit,
    modifier: Modifier = Modifier,
    autoScrollDelay: Duration = 5.seconds
) {
    val pagerState = rememberPagerState(pageCount = { banners.size })

    pagerState.AutoScroll(itemCount = banners.size, delay = autoScrollDelay)

    HorizontalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f)
    ) { index ->
        val banner = banners[index]
        Image(
            bitmap = banner.imageBitmap,
            contentDescription = "Slider Image $index",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .clip(MaterialTheme.shapes.large)
                .bounceClick {
                    banner.redirectUrl?.let {
                        onItemClick(it)
                    }
                }
        )
    }
}

@Composable
private fun PagerState.AutoScroll(itemCount: Int, delay: Duration) {
    if (itemCount <= 1) return

    LaunchedEffect(itemCount, delay) {
        while (isActive) {
            delay(delay)
            if (!isScrollInProgress) {
                animateScrollToPage((currentPage + 1) % itemCount)
            }
        }
    }
}

@Composable
fun bannerPlaceholder() = Banner(
    imageBitmap = ImageBitmap.imageResource(id = R.drawable.banner),
    redirectUrl = "https://t.me/yk_mahdi"
)

@Preview(showBackground = true)
@Composable
private fun BannerPagerPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            val sampleBanners = persistentListOf(
                bannerPlaceholder(),
                bannerPlaceholder(),
                bannerPlaceholder()
            )
            BannerPager(
                banners = sampleBanners,
                onItemClick = {}
            )
        }
    }
}