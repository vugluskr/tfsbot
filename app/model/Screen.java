package model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Denis Danilin | denis@danilin.name
 * 01.06.2020
 * tfs ☭ sweat and blood
 */
public class Screen {
    private int offset;
    private List<Object> elements;

    public Screen() {
        elements = new ArrayList<>(0);
    }

    public Object getElement(final int idx) {
        return elements.size() > idx ? elements.get(idx) : null;
    }

    public int addElement(final Object o) {
        if (o == null)
            return elements.size();

        elements.add(o);
        return elements.size() - 1;
    }

    public void reset() {
        offset = 0;
        elements.clear();
    }

    public int getOffset() {
        return offset;
    }

    public void pageUp() {
        offset += 10;
        elements.clear();
    }

    public void pageDown() {
        offset -= 10;
        elements.clear();
    }
}
