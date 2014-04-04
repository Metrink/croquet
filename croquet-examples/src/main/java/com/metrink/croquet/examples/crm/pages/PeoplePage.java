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
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.google.inject.Inject;
import com.metrink.croquet.examples.crm.CurrentUser;
import com.metrink.croquet.examples.crm.data.CompanyBean;
import com.metrink.croquet.examples.crm.data.PeopleBean;
import com.metrink.croquet.examples.crm.data.PeopleDataProvider;
import com.metrink.croquet.examples.crm.data.PeopleDataProvider.PeopleDataProviderFactory;

/**
 * A page to display all of the people in the CRM.
 */
public class PeoplePage extends AbstractFormPage<PeopleBean> {
    public static final String COMPANYID_PARAM = "companyid";

    private static final long serialVersionUID = -3930435318941508639L;

    private static final int TABLE_ROWS = 20;

    private final Integer companyId;

    // must add @Inject (and make the field transient) because CurrentUser isn't Serializable
    @Inject private final transient CurrentUser currentUser;

    /**
     * Constructor that uses Injection to obtain an instance of {@link PeopleDataProviderFactory}.
     * @param params the {@link PageParameters} passed to this page.
     * @param entityManager the {@link EntityManager} that reads/writes objects.
     * @param dataProviderFactory the {@link PeopleDataProvider} factory.
     * @param currentUser the "current user" of the site.
     */
    @Inject
    public PeoplePage(final PageParameters params,
                      final EntityManager entityManager,
                      final PeopleDataProviderFactory dataProviderFactory,
                      final CurrentUser currentUser) {
        super(PeopleBean.class, entityManager);

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

        // save off the company ID
        if(params.get(COMPANYID_PARAM).isEmpty()) {
            companyId = null;
        } else {
            companyId = params.get(COMPANYID_PARAM).toInteger();

            entityManager.getTransaction().begin();
            final CompanyBean companyBean = (CompanyBean)entityManager
                    .createQuery("select c from CompanyBean as c where c.companyId=" + companyId)
                    .getSingleResult();
            entityManager.getTransaction().commit();

            this.getFormModel().getObject().setCompany(companyBean);
        }

        final List<IColumn<PeopleBean, String>> columns = new ArrayList<IColumn<PeopleBean, String>>();

        // setup the edit link
        columns.add(new HeaderlessColumn<PeopleBean, String>() {

            private static final long serialVersionUID = -4881016875048427872L;

            @Override
            public void populateItem(final Item<ICellPopulator<PeopleBean>> item,
                                     final String componentId,
                                     final IModel<PeopleBean> rowModel) {
                final PeopleBean bean = rowModel.getObject();
                final Fragment linkFragment = new Fragment(componentId, "edit-fragment", PeoplePage.this);

                linkFragment.add(new AjaxLink<PeopleBean>("edit-link", rowModel) {
                    private static final long serialVersionUID = -1206619334473876487L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        PeoplePage.this.setForm(bean, target);
                    }
                });

                //linkFragment.add();
                item.add(linkFragment);
            }
        });

        // setup the columns for the table
        columns.add(new PropertyColumn<PeopleBean, String>(Model.of("Name"), "name", "name"));
        columns.add(new PropertyColumn<PeopleBean, String>(Model.of("Email"), "email", "email"));
        columns.add(new PropertyColumn<PeopleBean, String>(Model.of("Phone"), "phone", "phone"));
        columns.add(new PropertyColumn<PeopleBean, String>(Model.of("Company"), "company.name", "company.name"));

        // construct an instance of the data provider
        final PeopleDataProvider dataProvider = dataProviderFactory.create(companyId);

        add(new AjaxFallbackDefaultDataTable<PeopleBean, String>("people-table", columns, dataProvider, TABLE_ROWS));
    }

    @Override
    protected void addFormComponents(final Form<PeopleBean> form) {
        form.add(new TextField<String>("name", PropertyModel.<String>of(form.getModel(), "name")));
        form.add(new TextField<String>("email", PropertyModel.<String>of(form.getModel(), "email")));
        form.add(new TextField<String>("phone", PropertyModel.<String>of(form.getModel(), "phone")));
        form.add(new TextField<String>("company", PropertyModel.<String>of(form.getModel(), "company.name"))
                .setEnabled(false));
    }

    @Override
    protected PageParameters getRedirectPageParameters() {
        final PageParameters ret = new PageParameters();

        if(companyId != null) {
            ret.add(COMPANYID_PARAM, companyId);
        }

        return ret;
    }

}
