package pt.ist.fenixWebFramework.renderers.components;

public class HtmlCancelButton extends HtmlSubmitButton {

    public HtmlCancelButton() {
        super();

        setName(Constants.CANCEL_REQUESTED);
    }

    public HtmlCancelButton(String text) {
        this();
        setText(text);
    }
}
