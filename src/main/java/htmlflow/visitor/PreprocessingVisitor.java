/*
 * MIT License
 *
 * Copyright (c) 2014-2022, mcarvalho (gamboa.pt) and lcduarte (github.com/lcduarte)
 * and Pedro Fialho.
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

package htmlflow.visitor;

import htmlflow.HtmlView;
import org.xmlet.htmlapifaster.Element;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;

import static htmlflow.visitor.PreprocessingVisitor.HtmlContinuationSetter.setNext;

/**
 * This visitor is used to make a preprocessing resolution of an HtmlTemplate.
 * It will collect the resulting HTML from visiting static HTML elements into an auxiliary
 * StringBuilder that later is extracted to: String staticHtml = sb.substring(staticBlockIndex);
 * to create an HtmlContinuationStatic object.
 * It also interleaves the creation of HtmlContinuationDynamic nodes that only store dynamicHtmlBlock
 * objects corresponding to a BiConsumer<E, U> (being E an element and U the model).
 * The U comes from external module HtmlApiFaster whose classes are not strongly typed with the Model.
 * Thus, only the dynamic() and visitDynamic() methods in HtmlApiFaster were made generic to carry
 * a type parameter U corresponding to the type of the Model.
 * Nevertheless, this U should be corresponding to this visitor T of model that is parametrized in HtmlView.
 *
 * @param <T> The type of the Model bound to a view.
 */
public class PreprocessingVisitor<T> extends HtmlViewVisitor<T> implements TagsToStringBuilder {
    private static final String NOT_SUPPORTED_ERROR =
        "This is a PreprocessingVisitor used to compile templates and not intended to support HTML views!";

    /**
     * Flag to avoid nested dynamic blocks.
     */
    boolean openDynamic = false;
    /**
     * The main StringBuilder.
     */
    private final StringBuilder sb = new StringBuilder();
    /**
     * The internal String builder beginning index of a static HTML block.
     */
    private int staticBlockIndex = 0;
    /**
     * The first node to be processed.
     */
    private HtmlContinuation<T> first;
    /**
     * The last HtmlContinuation
     */
    private HtmlContinuation<T> last;
    /**
     * Used create a mocked instance of the model to be passed to dynamic HTML blocks.
     */
    private final Class<?> modelClass;
    /**
     * Generic type arguments of the Model.
     */
    private final Type[] genericTypeArgs;

    public PreprocessingVisitor(boolean isIndented, Class<?> modelClass, Type... genericTypeArgs) {
        super(isIndented);
        this.modelClass = modelClass;
        this.genericTypeArgs = genericTypeArgs;
    }

    public HtmlContinuation<T> getFirst() {
        return first;
    }

    @Override
    public void write(String text) {
        sb.append(text);
    }

    @Override
    protected void write(char c) {
        sb.append(c);
    }

    /**
     * Here we are creating 2 HtmlContinuation objects: one for previous static HTML and a next one
     * corresponding to the consumer passed to dynamic().
     * We will first create the dynamic continuation that will be the next node of the static continuation.
     *
     * U is the type of the model passed to the dynamic HTML block that is the same as T in this visitor.
     * Yet, since it came from HtmlApiFaster that is not typed by the Model, then we have to use
     * another generic argument for the type of the model.
     *
     * @param element The parent element.
     * @param dynamicHtmlBlock The continuation that consumes the element and a model.
     * @param <E> Type of the parent Element.
     * @param <U> Type of the model passed to the dynamic HTML block that is the same as T in this visitor.
     */
    @Override
    public <E extends Element, U> void visitDynamic(E element, BiConsumer<E, U> dynamicHtmlBlock) {
        if (openDynamic)
            throw new IllegalStateException("You are already in a dynamic block! Do not use dynamic() chained inside another dynamic!");
        openDynamic = true;
        /**
         * Creates an HtmlContinuation for the dynamic block.
         */
        HtmlContinuation<T> dynamicCont = (HtmlContinuation<T>) new HtmlContinuationDynamic<>(depth, isClosed, element, dynamicHtmlBlock, this, new HtmlContinuationCloseAndIndent(this));
        /**
         * We are resolving this view for the first time.
         * Now we just need to create an HtmlContinuation corresponding to the previous static HTML,
         * which will be followed by the dynamicCont.
         */
        String staticHtml = sb.substring(staticBlockIndex);
        String staticHtmlTrimmed = staticHtml.trim();  // trim to remove the indentation from static block
        HtmlContinuation<T> staticCont = new HtmlContinuationStatic<>(staticHtmlTrimmed, this, dynamicCont);
        if(first == null) first = staticCont; // on first visit initializes the first pointer
        else setNext(last, staticCont);       // else append the staticCont to existing chain
        last = dynamicCont.next;              // advance last to point to the new HtmlContinuationCloseAndIndent
        /**
         * We have to run dynamicContinuation to leave isClosed and indentation correct for
         * the next static HTML block.
         */
        newlineAndIndent();
        staticBlockIndex = sb.length(); // increment the staticBlockIndex to the end of internal string buffer.
        openDynamic = false;
    }

    /**
     * Creates the last static HTML block.
     */
    @Override
    public String finish(T model, HtmlView... partials) {
        String staticHtml = sb.substring(staticBlockIndex);
        HtmlContinuation<T> staticCont = new HtmlContinuationStatic<>(staticHtml.trim(), this, null);
        last = first == null
            ? first = staticCont         // assign both first and last
            : setNext(last, staticCont); // append new staticCont and return it to be the new last continuation.
        /**
         * We are just collecting static HTML blocks and the resulting HTML should be ignored.
         * Intentionally return null to force a NullPointerException if someone intend to use this result.
         */
        return null;
    }

    @Override
    public HtmlVisitor clone(PrintStream out, boolean isIndented) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_ERROR);
    }

    @Override
    public StringBuilder sb() {
        return sb;
    }

    @SuppressWarnings({"squid:S3011", "squid:S112"})
    static class HtmlContinuationSetter {
        private HtmlContinuationSetter() {
        }

        static final Field fieldNext;
        static {
            try {
                fieldNext = HtmlContinuation.class.getDeclaredField("next");
                fieldNext.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }
        static <Z> HtmlContinuation<Z> setNext(HtmlContinuation<Z> cont, HtmlContinuation<Z> next) {
            try {
                fieldNext.set(cont, next);
                return next;
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
