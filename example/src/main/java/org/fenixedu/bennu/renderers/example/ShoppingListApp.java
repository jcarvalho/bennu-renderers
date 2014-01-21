package org.fenixedu.bennu.renderers.example;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.presentationTier.actions.ContextBaseAction;
import org.fenixedu.bennu.portal.StrutsApplication;
import org.fenixedu.bennu.portal.StrutsFunctionality;
import org.fenixedu.bennu.renderers.example.domain.ShoppingList;
import org.fenixedu.bennu.renderers.example.domain.ShoppingListItem;

import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;
import pt.ist.fenixWebFramework.struts.annotations.Mapping;

@Mapping(path = "/shopping")
@StrutsApplication(path = "shopping", bundle = "resources.ExampleResources",
        descriptionKey = "title.example.shoppinglist.description", titleKey = "title.example.shoppinglist",
        accessGroup = "anyone", parent = RenderersExampleApplication.class)
@StrutsFunctionality(application = ShoppingListApp.class, bundle = "resources.ExampleResources",
        descriptionKey = "title.example.shoppinglist.list.description", path = "list",
        titleKey = "title.example.shoppinglist.list")
public class ShoppingListApp extends ContextBaseAction {

    public ActionForward list(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        request.setAttribute("list", Bennu.getInstance().getShoppingListSet());
        return forward(request, "/example/shoppinglist.jsp");
    }

    public ActionForward create(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        return forward(request, "/example/createShoppinglist.jsp");
    }

    public ActionForward view(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        ShoppingList list = getDomainObject(request, "listId");
        request.setAttribute("list", list);
        return forward(request, "/example/viewShoppinglist.jsp");
    }

    public ActionForward delete(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        return list(mapping, form, request, response);
    }

    public ActionForward addItem(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        ShoppingListItem item = getRenderedObject("create");
        RenderUtils.invalidateViewState();
        request.setAttribute("list", item.getShoppingList());
        return forward(request, "/example/viewShoppinglist.jsp");
    }

}
