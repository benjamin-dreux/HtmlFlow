/*
 * MIT License
 *
 * Copyright (c) 2014-18, mcarvalho (gamboa.pt) and lcduarte (github.com/lcduarte)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package htmlflow;

import java.io.PrintStream;
import java.util.function.BiConsumer;

/**
 * Dynamic views can be bound to a domain object.
 *
 * @param <T> The type of domain object bound to this View.
 *
 * @author Miguel Gamboa, Luís Duare
 */
public class DynamicHtml<T> extends HtmlView<T> {

    private static final String WRONG_USE_OF_RENDER_WITHOUT_MODEL =
             "Wrong use of DynamicView! You should provide a " +
             "model parameter or use a static view instead!";
    /**
     * Used alternately with the field binder.
     * A template function receives 3 arguments:
     *   the view, the domain object and a varargs array of partial views.
     */
    private HtmlTemplate<T> template;
    /**
     * Used alternately with the field template.
     * A binder function is responsible for binding the View with a domain object.
     * Thus, it is a function that receives two arguments: the view and the domain object.
     */
    private BiConsumer<DynamicHtml<T>, T> binder;

    public static <U> DynamicHtml<U> view(PrintStream out, HtmlTemplate<U> template){
        return new DynamicHtml<>(out, template);
    }

    public static <U> DynamicHtml<U> view(PrintStream out, BiConsumer<DynamicHtml<U>, U> binder){
        return new DynamicHtml<>(out, binder);
    }

    public static <U> DynamicHtml<U> view(HtmlTemplate<U> template){
        return new DynamicHtml<>(template);
    }

    public static <U> DynamicHtml<U> view(BiConsumer<DynamicHtml<U>, U> binder) {
        return new DynamicHtml<>(binder);
    }

    private DynamicHtml(PrintStream out, HtmlTemplate<T> template) {
        this.visitor = ThreadLocal.withInitial(() -> new HtmlVisitorPrintStream(out, true));
        this.template = template;
    }

    private DynamicHtml(PrintStream out, BiConsumer<DynamicHtml<T>, T> binder) {
        this.visitor = ThreadLocal.withInitial(() -> new HtmlVisitorPrintStream(out, true));
        this.binder = binder;
    }

    private DynamicHtml(HtmlTemplate<T> template) {
        this.visitor = ThreadLocal.withInitial(() -> new HtmlVisitorStringBuilder(true));
        this.template = template;
    }

    private DynamicHtml(BiConsumer<DynamicHtml<T>, T> binder) {
        this.visitor = ThreadLocal.withInitial(() -> new HtmlVisitorStringBuilder(true));
        this.binder = binder;
    }

    @Override
    public String render() {
        if(getVisitor() instanceof HtmlVisitorPrintStream)
            throw new IllegalStateException(WRONG_USE_OF_RENDER_WITH_PRINTSTREAM);
        throw new UnsupportedOperationException(WRONG_USE_OF_RENDER_WITHOUT_MODEL);
    }

    @Override
    public String render(T model) {
        if(getVisitor() instanceof HtmlVisitorPrintStream)
            throw new IllegalStateException(WRONG_USE_OF_RENDER_WITH_PRINTSTREAM);
        binder.accept(this, model);
        return getVisitor().finished();
    }

    public String render(T model, HtmlView...partials) {
        if(getVisitor() instanceof HtmlVisitorPrintStream)
            throw new IllegalStateException(WRONG_USE_OF_RENDER_WITH_PRINTSTREAM);
        template.resolve(this, model, partials);
        return getVisitor().finished();
    }

    @Override
    public void write() {
        throw new UnsupportedOperationException(WRONG_USE_OF_RENDER_WITHOUT_MODEL);
    }

    @Override
    public void write(T model) {
        binder.accept(this, model);
        getVisitor().finished();
    }

    public void write(T model, HtmlView...partials) {
        template.resolve(this, model, partials);
        getVisitor().finished();
    }
}
