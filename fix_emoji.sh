#!/bin/bash
sed -i '/fun EmojiOverlay(theme: ThemeEntity, onEmojiSelected: (String) -> Unit, onClose: () -> Unit) {/,/}/c\
@Composable\
fun EmojiOverlay(theme: ThemeEntity, onEmojiSelected: (String) -> Unit, onClose: () -> Unit) {\
    val emojis = listOf(\
        "😀", "😂", "🤣", "😊", "😍", "🥰", "😎", "🤔", "😅", "😆", "😉", "😋",\
        "😘", "😗", "😙", "😚", "🙂", "🤗", "🤩", "😶", "🙄", "😏", "😣", "😥",\
        "😮", "🤐", "😯", "😪", "😫", "🥱", "😴", "😌", "😛", "😜", "😝", "🤤",\
        "😒", "😓", "😔", "😕", "🙃", "🤑", "😲", "☹️", "🙁", "😖", "😞", "😟",\
        "😤", "😢", "😭", "😦", "😧", "😨", "😩", "🤯", "😬", "😰", "😱", "🥵",\
        "🥶", "😳", "🤪", "😵", "😡", "😠", "🤬", "😷", "🤒", "🤕", "🤢", "🤮",\
        "🤧", "😇", "🥳", "🥺", "🤠", "🤡", "🤥", "🤫", "🤭", "🧐", "🤓", "😈"\
    )\
\
    Column(modifier = Modifier.fillMaxWidth().height(250.dp).background(Color(theme.backgroundColor))) {\
        Row(\
            modifier = Modifier.fillMaxWidth().height(40.dp).background(Color(theme.activeKeyBackgroundColor)),\
            horizontalArrangement = Arrangement.SpaceBetween,\
            verticalAlignment = Alignment.CenterVertically\
        ) {\
            Text(\
                text = "Emojis", \
                color = Color(theme.keyTextColor),\
                modifier = Modifier.padding(start = 16.dp),\
                fontWeight = FontWeight.Bold\
            )\
            Box(\
                modifier = Modifier.clickable { onClose() }.padding(end = 16.dp, top = 8.dp, bottom = 8.dp)\
            ) {\
                Text("Close", color = Color(theme.keyTextColor))\
            }\
        }\
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(\
            columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 40.dp),\
            contentPadding = PaddingValues(8.dp),\
            modifier = Modifier.fillMaxSize()\
        ) {\
            items(emojis.size) { index ->\
                Box(\
                    modifier = Modifier\
                        .size(40.dp)\
                        .clickable { onEmojiSelected(emojis[index]) },\
                    contentAlignment = Alignment.Center\
                ) {\
                    Text(text = emojis[index], fontSize = 24.sp)\
                }\
            }\
        }\
    }\
}\
' app/src/main/java/com/example/keyboard/KeyboardComponents.kt
