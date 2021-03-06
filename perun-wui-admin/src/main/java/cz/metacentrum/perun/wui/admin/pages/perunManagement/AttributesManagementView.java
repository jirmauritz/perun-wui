package cz.metacentrum.perun.wui.admin.pages.perunManagement;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewImpl;
import cz.metacentrum.perun.wui.client.utils.JsUtils;
import cz.metacentrum.perun.wui.client.utils.UiUtils;
import cz.metacentrum.perun.wui.json.JsonEvents;
import cz.metacentrum.perun.wui.json.managers.AttributesManager;
import cz.metacentrum.perun.wui.model.PerunException;
import cz.metacentrum.perun.wui.model.beans.AttributeDefinition;
import cz.metacentrum.perun.wui.model.columnProviders.AttributeDefinitionColumnProvider;
import cz.metacentrum.perun.wui.pages.FocusableView;
import cz.metacentrum.perun.wui.widgets.PerunButton;
import cz.metacentrum.perun.wui.widgets.PerunDataGrid;
import cz.metacentrum.perun.wui.widgets.boxes.ExtendedSuggestBox;
import cz.metacentrum.perun.wui.widgets.resources.PerunButtonType;
import cz.metacentrum.perun.wui.widgets.resources.PerunColumnType;
import cz.metacentrum.perun.wui.widgets.resources.UnaccentMultiWordSuggestOracle;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ButtonToolBar;
import org.gwtbootstrap3.client.ui.constants.ButtonType;

import java.util.HashMap;

/**
 * PERUN ADMIN - ATTRIBUTES MANAGEMENT VIEW
 *
 * @author Pavel Zlámal <zlamal@cesnet.cz>
 */
public class AttributesManagementView extends ViewImpl implements AttributesManagementPresenter.MyView, FocusableView {

	private UnaccentMultiWordSuggestOracle oracle = new UnaccentMultiWordSuggestOracle(" .-:");

	@UiField(provided = true)
	PerunDataGrid<AttributeDefinition> grid;

	@UiField (provided = true)
	PerunButton remove;

	@UiField(provided = true)
	ExtendedSuggestBox textBox = new ExtendedSuggestBox(oracle);

	@UiField ButtonToolBar menu;
	@UiField PerunButton filterButton;
	@UiField PerunButton button;
	@UiField AnchorListItem idDropdown;
	@UiField AnchorListItem nameDropdown;
	@UiField AnchorListItem descriptionDropdown;
	@UiField AnchorListItem entityDropdown;
	@UiField AnchorListItem definitionDropdown;
	@UiField AnchorListItem typeDropdown;
	@UiField Button dropdown;
	private HashMap<AnchorListItem, PerunColumnType> anchorColumnMap;

	AttributesManagementView view = this;

	interface AttributesManagementViewUiBinder extends UiBinder<Widget, AttributesManagementView> {
	}

	@Inject
	AttributesManagementView(final AttributesManagementViewUiBinder uiBinder) {

		AttributeDefinitionColumnProvider provider = new AttributeDefinitionColumnProvider();
		grid = new PerunDataGrid<AttributeDefinition>(provider);
		remove = PerunButton.getButton(PerunButtonType.REMOVE, ButtonType.DANGER, "Remove selected Attributes");
		initWidget(uiBinder.createAndBindUi(this));
		anchorColumnMap = new HashMap<AnchorListItem, PerunColumnType>();
		anchorColumnMap.put(idDropdown, PerunColumnType.ID);
		anchorColumnMap.put(nameDropdown, PerunColumnType.ATTR_FRIENDLY_NAME);
		anchorColumnMap.put(descriptionDropdown, PerunColumnType.DESCRIPTION);
		anchorColumnMap.put(entityDropdown, PerunColumnType.ATTR_ENTITY);
		anchorColumnMap.put(definitionDropdown, PerunColumnType.ATTR_DEF);
		anchorColumnMap.put(typeDropdown, PerunColumnType.ATTR_TYPE);

		UiUtils.bindFilterBox(grid, textBox, filterButton);
		UiUtils.bindFilteringDropDown(anchorColumnMap, grid);
		UiUtils.bindTableLoading(grid, filterButton, true);
		UiUtils.bindTableLoading(grid, textBox, true);
		UiUtils.bindTableLoading(grid, dropdown, true);
		UiUtils.bindTableSelection(grid, remove);

		draw();
	}

	@UiHandler(value = "remove")
	public void removeOnClick(ClickEvent event) {

	}

	@UiHandler(value = "button")
	public void onClick(ClickEvent event) {
		// FIXME - for testing of table reload only
		draw();
	}

	public void draw() {

		AttributesManager.getAttributesDefinitions(new JsonEvents() {

			JsonEvents loadAgain = this;

			@Override
			public void onFinished(JavaScriptObject jso) {
				grid.setList(JsUtils.<AttributeDefinition>jsoAsList(jso));
				for (AttributeDefinition attr : grid.getList()) {
					oracle.add(attr.getURN());
					oracle.add(attr.getName());
				}
			}

			@Override
			public void onError(PerunException error) {
				grid.getLoaderWidget().onError(error, new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						AttributesManager.getAttributesDefinitions(loadAgain);
					}
				});
			}

			@Override
			public void onLoadingStart() {
				grid.clearTable();
				grid.getLoaderWidget().onLoading();
			}
		});

	}

	@Override
	public void focus() {
		textBox.setFocus(true);
	}

}