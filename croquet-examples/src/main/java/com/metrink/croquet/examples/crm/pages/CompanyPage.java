package com.metrink.croquet.examples.crm.pages;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeaderlessColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.google.inject.Inject;
import com.metrink.croquet.examples.crm.CurrentUser;
import com.metrink.croquet.examples.crm.data.CompanyBean;
import com.metrink.croquet.examples.crm.data.GenericDataProvider;
import com.metrink.croquet.examples.crm.data.GenericDataProvider.GenericDataProviderFactory;

/**
 * A page to display all of the companies in the CRM.
 */
public class CompanyPage extends AbstractFormPage<CompanyBean> {
    private static final long serialVersionUID = -1529533206724745674L;

    private static final int TABLE_ROWS = 20;

    // must add @Inject (and make the field transient) because CurrentUser isn't Serializable
    @Inject private final transient CurrentUser currentUser;

    /**
     * Constructor for the {@link CompanyPage}.
     * @param entityManager the {@EntityManager} to read/write objects.
     * @param dataProviderFactory a data provider factory to fill out the table.
     * @param currentUser the "current user" of the site.
     */
    @Inject
    public CompanyPage(final EntityManager entityManager,
                       final GenericDataProviderFactory<CompanyBean> dataProviderFactory,
                       final CurrentUser currentUser) {
        super(CompanyBean.class, entityManager);

        /*
         * This is a VERY contrived example of injecting a non-Serializable dependency.
         * We created the "dummy" class CurrentUser to show how to deal with non-Serializable fields.
         * Granted, we don't even need to save the CurrentUser object in a field, constructor injection
         * in this case is enough.
         *
         * The steps are as follows:
         * 1) Inject the dependency via the constructor
         * 2) Call Injector.get().inject(this)
         * 3) Ensure the field is marked as transient with @Inject
         * 4) Save the reference from the constructor into the field
         */
        Injector.get().inject(this);

        this.currentUser = currentUser;

        add(new Label("username", this.currentUser.getCurrentUser()));

        final List<IColumn<CompanyBean, String>> columns = new ArrayList<IColumn<CompanyBean, String>>();

        // setup the edit link
        columns.add(new HeaderlessColumn<CompanyBean, String>() {

            private static final long serialVersionUID = -4881016875048427872L;

            @Override
            public void populateItem(final Item<ICellPopulator<CompanyBean>> item,
                                     final String componentId,
                                     final IModel<CompanyBean> rowModel) {
                final CompanyBean bean = rowModel.getObject();
                final Fragment linkFragment = new Fragment(componentId, "edit-fragment", CompanyPage.this);

                linkFragment.add(new AjaxLink<CompanyBean>("edit-link", rowModel) {
                    private static final long serialVersionUID = -1206619334473876487L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        CompanyPage.this.setForm(bean, target);
                    }
                });

                //linkFragment.add();
                item.add(linkFragment);
            }
        });

        // setup the name column as a link to the people page
        columns.add(new PropertyColumn<CompanyBean, String>(Model.of("Name"), "name", "name") {

            private static final long serialVersionUID = -4881016875048427872L;

            @Override
            public void populateItem(final Item<ICellPopulator<CompanyBean>> item,
                                     final String componentId,
                                     final IModel<CompanyBean> rowModel) {
                final CompanyBean bean = rowModel.getObject();
                final Fragment linkFragment = new Fragment(componentId, "name-fragment", CompanyPage.this);

                linkFragment.add(new Link<CompanyBean>("name-link", rowModel) {
                    private static final long serialVersionUID = -1206619334473876487L;

                    @Override
                    public void onClick() {
                        final PageParameters params =
                                new PageParameters().add(PeoplePage.COMPANYID_PARAM, bean.getCompanyId());

                        // pass any values via params
                        setResponsePage(PeoplePage.class, params);
                    }
                }.add(new Label("name", bean.getName())));

                //linkFragment.add();
                item.add(linkFragment);
            }
        });

        columns.add(new PropertyColumn<CompanyBean, String>(Model.of("Street"), "street", "street"));
        columns.add(new PropertyColumn<CompanyBean, String>(Model.of("City"), "city", "city"));
        columns.add(new PropertyColumn<CompanyBean, String>(Model.of("State"), "state", "state"));
        columns.add(new PropertyColumn<CompanyBean, String>(Model.of("Zip"), "zip", "zip"));

        // construct an instance of the GenericDataProvider
        final GenericDataProvider<CompanyBean> dataProvider = dataProviderFactory.create(CompanyBean.class);

        add(new AjaxFallbackDefaultDataTable<CompanyBean, String>("company-table", columns, dataProvider, TABLE_ROWS));
    }

    @Override
    protected void addFormComponents(final Form<CompanyBean> form) {
        form.add(new TextField<String>("name", PropertyModel.<String>of(form.getModel(), "name")));
        form.add(new TextField<String>("street", PropertyModel.<String>of(form.getModel(), "street")));
        form.add(new TextField<String>("city", PropertyModel.<String>of(form.getModel(), "city")));
        form.add(new TextField<String>("state", PropertyModel.<String>of(form.getModel(), "state")));
        form.add(new TextField<String>("zip", PropertyModel.<String>of(form.getModel(), "zip")));
    }

}
