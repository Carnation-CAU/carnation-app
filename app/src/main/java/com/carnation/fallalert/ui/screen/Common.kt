package com.carnation.fallalert.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carnation.fallalert.ui.theme.Elevation
import com.carnation.fallalert.ui.theme.Hairline
import com.carnation.fallalert.ui.theme.Radius
import com.carnation.fallalert.ui.theme.Space
import com.carnation.fallalert.ui.theme.TouchTarget

/**
 * 회색 배경 위의 흰 카드. 그림자 없이 색 대비로만 분리한다.
 *
 * 카드에 그림자를 얹지 않는 게 이 디자인의 기본 규칙이다 — 회색 바탕과 흰 면의 차이로
 * 이미 층이 생기고, 거기 그림자를 더하면 화면이 무거워진다.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    contentPadding: androidx.compose.ui.unit.Dp = Space.xl,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = Radius.card
    val base = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(color)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Column(modifier = base.padding(contentPadding), content = content)
}

/**
 * 리스트 행: 원형 아이콘 + 제목(볼드) + 부제(회색) + 화살표.
 * 행 사이에 구분선을 두지 않는다 — 여백으로만 나눈다.
 */
@Composable
fun IconListRow(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    showChevron: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TouchTarget.minimum),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }

        Spacer(Modifier.width(Space.lg))

        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        trailing?.invoke(this)

        if (showChevron) {
            Spacer(Modifier.width(Space.sm))
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** 확인 결과 같은 상태 표시. 작고 조용하게. */
@Composable
fun StatusBadge(
    text: String,
    contentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(Radius.pill)
            .background(containerColor)
            .padding(horizontal = Space.md, vertical = Space.xs),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
        )
    }
}

/** 라벨 위, 값 아래. 큰 수치를 보여줄 때 쓰는 기본 조합. */
@Composable
fun LabeledValue(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    valueStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.displayMedium,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Space.xs))
        Text(text = value, style = valueStyle, color = valueColor)
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(bottom = Space.md),
    )
}

/** 얇은 구분선. 카드 안에서 항목을 나눌 때만 쓴다. */
@Composable
fun HairlineDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Hairline),
    )
}

/** 하단 풀와이드 주요 버튼. 블루 배경 + 흰 볼드. 화면당 하나. */
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    /** 아웃라인 버튼과 나란히 둘 때, 같은 두께의 테두리로 형태를 맞추기 위한 값. */
    borderColor: Color? = null,
    horizontalPadding: androidx.compose.ui.unit.Dp = Space.md,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = Radius.button,
        border = borderColor?.let { androidx.compose.foundation.BorderStroke(1.dp, it) },
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = horizontalPadding, vertical = Space.sm,
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TouchTarget.primaryAction),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Space.sm))
        }
        Text(text, maxLines = 1)
    }
}

/** 보조 버튼. 아웃라인 + 얇은 회색 테두리. */
@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    borderColor: Color = Hairline,
    horizontalPadding: androidx.compose.ui.unit.Dp = Space.md,
) {
    OutlinedButton(
        onClick = onClick,
        shape = Radius.button,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = contentColor,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = horizontalPadding, vertical = Space.sm,
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TouchTarget.primaryAction),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Space.sm))
        }
        Text(text, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** 화면 상단 큰 타이틀. 배경과 같은 색이라 바처럼 보이지 않는다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTitle(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
            )
        },
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = modifier,
    )
}

/** 하단 고정 영역. 여기만 아주 옅은 그림자를 허용한다 — 콘텐츠가 실제로 밑을 지나가므로. */
@Composable
fun BottomBarSurface(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        shadowElevation = Elevation.subtle,
    ) {
        Column(
            modifier = Modifier.padding(
                start = Space.xl, end = Space.xl, top = Space.md, bottom = Space.lg,
            ),
            verticalArrangement = Arrangement.spacedBy(Space.sm),
            content = content,
        )
    }
}
