# Artist-drawn sprites (optional)

Every character, the palm, the hut and the "!" / "?" markers are drawn in code (`isla.ui.Sprites`), so the
game runs with this folder empty. To replace one with hand-drawn art, drop a PNG here named after the sprite.
If the file exists and is valid it is used; otherwise the code-drawn sprite is used. No code changes needed.

| File | What it is |
|------|------------|
| `player.png` | The player. 32x16 = two walking frames side by side (16x16 = one frame, no bob) |
| `mara.png` | Mara, the fruit seller |
| `tomas.png` | Tomas, the tool seller |
| `yara.png` | Yara, the jungle trader |
| `elder_kai.png` | Elder Kai |
| `coder_nia.png` | Coder Nia |
| `palm.png` | A palm tree (drawn over a grass tile) |
| `hut.png` | A hut (drawn over a grass tile) |
| `exclaim.png` | The "!" shop marker |
| `question.png` | The "?" quest marker |

Rules: PNG, exactly 16 pixels tall, and 16 x N pixels wide for N frames (frames side by side, left to right).
Use transparency for the background. A file that is the wrong size or cannot be read is ignored (the code-drawn
sprite shows instead), so a bad export never breaks the game. Pictures are enlarged with no smoothing, so keep
the art crisp. Sprites are read once, the first time they are drawn, so restart the game after changing a PNG.
