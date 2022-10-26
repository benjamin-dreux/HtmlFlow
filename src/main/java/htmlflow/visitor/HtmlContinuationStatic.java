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

/**
 * @param <U> the type of the template's model.
 */
public class HtmlContinuationStatic<U> extends HtmlContinuation<U> {
    final String staticHtmlBlock;
    /**
     * Sets indentation to -1 to inform that visitor should continue with previous indentation.
     * The isClosed is useless because it just writes what it is in its staticHtmlBlock.
     */
    HtmlContinuationStatic(String staticHtmlBlock, HtmlVisitor visitor, HtmlContinuation<U> next) {
        super(-1, false, visitor, next); // The isClosed parameter is useless in this case of Static HTML block.
        this.staticHtmlBlock = staticHtmlBlock;
    }

    @Override
    protected void emitHtml(U model) {
        visitor.write(staticHtmlBlock);
    }

    @Override
    protected HtmlContinuation<U> copy(HtmlVisitor v) {
        return new HtmlContinuationStatic<>(
            staticHtmlBlock,
            v,
            next != null ? next.copy(v) : null); // call copy recursively
    }
}
