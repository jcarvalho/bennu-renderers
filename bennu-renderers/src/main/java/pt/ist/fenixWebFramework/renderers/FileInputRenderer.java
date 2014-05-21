package pt.ist.fenixWebFramework.renderers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletException;
import javax.servlet.http.Part;

import org.apache.commons.beanutils.PropertyUtils;

import pt.ist.fenixWebFramework.renderers.components.HtmlComponent;
import pt.ist.fenixWebFramework.renderers.components.HtmlForm;
import pt.ist.fenixWebFramework.renderers.components.HtmlInputFile;
import pt.ist.fenixWebFramework.renderers.components.HtmlSimpleValueComponent;
import pt.ist.fenixWebFramework.renderers.components.controllers.HtmlController;
import pt.ist.fenixWebFramework.renderers.components.converters.ConversionException;
import pt.ist.fenixWebFramework.renderers.components.converters.Converter;
import pt.ist.fenixWebFramework.renderers.components.state.IViewState;
import pt.ist.fenixWebFramework.renderers.layouts.Layout;
import pt.ist.fenixWebFramework.renderers.model.MetaObject;
import pt.ist.fenixWebFramework.renderers.model.MetaSlot;
import pt.ist.fenixWebFramework.renderers.model.MetaSlotKey;
import pt.ist.fenixWebFramework.renderers.plugin.RenderersRequestMapper;

/**
 * This renderer creates a file input field that allows the user to submit a
 * file. The renderer will set the slot with an output stream that represents
 * the file.
 * 
 * <p>
 * Example: <input type="file"/>
 * 
 * @author cfgi
 */
public class FileInputRenderer extends InputRenderer {

    private String size;
    private String fileNameSlot;
    private String fileSizeSlot;
    private String fileContentTypeSlot;
    private String onChangeEvent;

    public String getSize() {
        return this.size;
    }

    /**
     * Selects the size of the file input field.
     * 
     * @property
     */
    public void setSize(String size) {
        this.size = size;
    }

    public String getFileContentTypeSlot() {
        return this.fileContentTypeSlot;
    }

    /**
     * When this property is specified the renderer will set the given slot with
     * the content type of the file. The slot must have the string type.
     * 
     * @property
     */
    public void setFileContentTypeSlot(String fileContentTypeSlot) {
        this.fileContentTypeSlot = fileContentTypeSlot;
    }

    public String getFileNameSlot() {
        return this.fileNameSlot;
    }

    /**
     * When this property is specified the renderer will set the given slot with
     * the name of the file as given by the user. The slot must have the string
     * type.
     * 
     * @property
     */
    public void setFileNameSlot(String fileNameSlot) {
        this.fileNameSlot = fileNameSlot;
    }

    public String getFileSizeSlot() {
        return this.fileSizeSlot;
    }

    /**
     * When this property is specified the renderer will set the given slot with
     * the size of the file in bytes. The slot must be have the <code>long</code> type.
     * 
     * @property
     */
    public void setFileSizeSlot(String fileSizeSlot) {
        this.fileSizeSlot = fileSizeSlot;
    }

    public String getOnChangeEvent() {
        return this.onChangeEvent;
    }

    /**
     * JavaScript statement to call when <b>Change</b> event is triggered.
     * This can be used, for example, to model a behavior such like having
     * this <code><b>input</b></code> element only allowing the submit <code><b>button</b></code> to be enabled when a valid input
     * is selected.
     * 
     * @property
     */
    public void setOnChangeEvent(String onChangeEvent) {
        this.onChangeEvent = onChangeEvent;
    }

    @Override
    protected Layout getLayout(Object object, Class type) {
        return new Layout() {

            @Override
            public HtmlComponent createComponent(Object object, Class type) {
                getInputContext().getForm().setEncoding(HtmlForm.FORM_DATA);

                HtmlInputFile file = new HtmlInputFile();
                file.setTargetSlot((MetaSlotKey) getInputContext().getMetaObject().getKey());

                file.setController(new UpdateFilePropertiesController(((MetaSlot) getInputContext().getMetaObject())
                        .getMetaObject(), getFileNameSlot(), getFileSizeSlot(), getFileContentTypeSlot()));
                file.setConverter(new FileConverter(file));
                if (getOnChangeEvent() != null) {
                    file.setAttribute("onchange", getOnChangeEvent());
                }
                return file;
            }

            @Override
            public void applyStyle(HtmlComponent component) {
                super.applyStyle(component);

                ((HtmlInputFile) component).setSize(getSize());
            }

        };
    }

    private static class UpdateFilePropertiesController extends HtmlController {

        private final MetaObject object;
        private final String fileNameSlot;
        private final String fileSizeSlot;
        private final String fileContentTypeSlot;

        public UpdateFilePropertiesController(MetaObject object, String fileNameSlot, String fileSizeSlot,
                String fileContentTypeSlot) {
            this.object = object;
            this.fileNameSlot = fileNameSlot;
            this.fileSizeSlot = fileSizeSlot;
            this.fileContentTypeSlot = fileContentTypeSlot;
        }

        @Override
        public void execute(IViewState viewState) {
            HtmlSimpleValueComponent component = (HtmlSimpleValueComponent) getControlledComponent();
            String name = component.getName();

            Part file = RenderersRequestMapper.getUploadedFile(name);
            if (file != null) { // if has file
                Object object = this.object.getObject();

                try {
                    String currentEncoding = RenderersRequestMapper.getCurrentRequest().getCharacterEncoding();
                    setPropertyIgnoringErrors(object, this.fileNameSlot, currentEncoding != null ? new String(getFileName(file)
                            .getBytes(), currentEncoding) : new String(getFileName(file).getBytes()));
                } catch (UnsupportedEncodingException e) {
                    // best effort name setting
                    setPropertyIgnoringErrors(object, this.fileNameSlot, getFileName(file));
                    e.printStackTrace();
                }
                setPropertyIgnoringErrors(object, this.fileSizeSlot, file.getSize());
                setPropertyIgnoringErrors(object, this.fileContentTypeSlot, file.getContentType());
            }
        }

        public static String getFileName(Part filePart) {
            String header = filePart.getHeader("content-disposition");
            for (String headerPart : header.split(";")) {
                if (headerPart.trim().startsWith("filename")) {
                    return headerPart.substring(headerPart.indexOf('=') + 1).trim().replace("\"", "");
                }
            }
            return null;
        }

        private void setPropertyIgnoringErrors(Object object, String property, Object value) {
            if (property == null) {
                return;
            }

            try {
                PropertyUtils.setProperty(object, property, value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class FileConverter extends Converter {

        private final HtmlInputFile component;

        public FileConverter(HtmlInputFile file) {
            this.component = file;
        }

        @Override
        public Object convert(Class type, Object value) {
            String name = this.component.getName();

            try {
                Part file = RenderersRequestMapper.getCurrentRequest().getPart(name);
                if (file == null) {
                    return null;
                } else {
                    return file.getInputStream();
                }
            } catch (IOException | ServletException e) {
                throw new ConversionException("renderers.converter.file.obtain", e, true, (Object[]) null);
            }
        }

    }
}
