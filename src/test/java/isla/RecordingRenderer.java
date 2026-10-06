package isla;

import java.util.ArrayList;
import java.util.List;

/** Test double: records every draw call as a string so tests can assert on them. */
public class RecordingRenderer implements Renderer {

    public final List<String> calls = new ArrayList<>();
    public final List<String> texts = new ArrayList<>();

    @Override
    public void setCamera(int col, int row) {
        calls.add("camera " + col + "," + row);
    }

    @Override
    public void drawTile(int col, int row, String hexColor) {
        calls.add("tile " + col + "," + row + "," + hexColor);
    }

    @Override
    public void drawSprite(int col, int row, String spriteKey) {
        calls.add("sprite " + col + "," + row + "," + spriteKey);
    }

    @Override
    public void drawLabel(int col, int row, String text) {
        calls.add("label " + col + "," + row + "," + text);
    }

    @Override
    public void fillRect(int px, int py, int width, int height, String hexColor) {
        calls.add("rect " + px + "," + py + "," + width + "," + height + "," + hexColor);
    }

    @Override
    public void drawText(int px, int py, String text, String hexColor) {
        calls.add("text " + px + "," + py + "," + text);
        texts.add(text);
    }

    public long count(String prefix) {
        return calls.stream().filter(c -> c.startsWith(prefix)).count();
    }

    public String lastSprite() {
        for (int i = calls.size() - 1; i >= 0; i--) {
            if (calls.get(i).startsWith("sprite ")) return calls.get(i);
        }
        return null;
    }

    public boolean anyTextContains(String fragment) {
        return texts.stream().anyMatch(t -> t.contains(fragment));
    }
}
