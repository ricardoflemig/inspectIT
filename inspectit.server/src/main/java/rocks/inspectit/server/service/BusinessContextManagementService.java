package rocks.inspectit.server.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import rocks.inspectit.shared.all.communication.data.cmr.ApplicationData;
import rocks.inspectit.shared.all.communication.data.cmr.BusinessTransactionData;
import rocks.inspectit.shared.all.spring.logger.Log;
import rocks.inspectit.shared.all.util.Pair;
import rocks.inspectit.shared.cs.ci.business.impl.ApplicationDefinition;
import rocks.inspectit.shared.cs.ci.business.impl.BusinessTransactionDefinition;
import rocks.inspectit.shared.cs.cmr.service.IBusinessContextManagementService;
import rocks.inspectit.shared.cs.cmr.service.IBusinessContextRegistryService;

/**
 * Cached access and management service to the business context definition.
 *
 * @author Alexander Wert
 *
 */
@Service
public class BusinessContextManagementService implements IBusinessContextManagementService, IBusinessContextRegistryService, InitializingBean {
	/** The logger of this class. */
	@Log
	Logger log;

	/**
	 * Map of {@link ApplicationData} instances representing identified applications.
	 */
	private final ConcurrentHashMap<Integer, ApplicationData> applications = new ConcurrentHashMap<Integer, ApplicationData>();

	/**
	 * Map of {@link BusinessTransactionData} instances representing identified business
	 * transactions.
	 */
	private final ConcurrentHashMap<Pair<Integer, Integer>, BusinessTransactionData> businessTransactions = new ConcurrentHashMap<Pair<Integer, Integer>, BusinessTransactionData>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<ApplicationData> getApplications() {
		return new ArrayList<>(applications.values());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<BusinessTransactionData> getBusinessTransactions() {
		return new ArrayList<>(businessTransactions.values());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<BusinessTransactionData> getBusinessTransactions(int applicationId) {
		Set<BusinessTransactionData> resultSet = new HashSet<>();
		for (BusinessTransactionData businessTx : businessTransactions.values()) {
			if (businessTx.getApplication().getId() == applicationId) {
				resultSet.add(businessTx);
			}
		}
		return resultSet;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ApplicationData registerApplication(ApplicationDefinition applicationDefinition) {
		int applicationId = deriveInstanceId(applicationDefinition.getApplicationName(), applicationDefinition.getId());
		ApplicationData application = getApplicationForId(applicationId);
		if (null == application) {
			application = new ApplicationData(applicationId, applicationDefinition.getId(), applicationDefinition.getApplicationName());
			ApplicationData existingApplication = applications.putIfAbsent(applicationId, application);
			if (null != existingApplication) {
				application = existingApplication;
			}
		}

		return application;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BusinessTransactionData registerBusinessTransaction(ApplicationData application, BusinessTransactionDefinition businessTransactionDefinition, String businessTransactionName) {
		int businessTxId = deriveInstanceId(businessTransactionName, businessTransactionDefinition.getId());
		BusinessTransactionData businessTransaction = getBusinessTransactionForId(application.getId(), businessTxId);
		if (null == businessTransaction) {
			businessTransaction = new BusinessTransactionData(businessTxId, businessTransactionDefinition.getId(), application, businessTransactionName);
			Pair<Integer, Integer> key = new Pair<Integer, Integer>(application.getId(), businessTransaction.getId());
			BusinessTransactionData existingBusinessTx = businessTransactions.putIfAbsent(key, businessTransaction);
			if (null != existingBusinessTx) {
				businessTransaction = existingBusinessTx;
			}
		}

		return businessTransaction;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ApplicationData getApplicationForId(int id) {
		return applications.get(id);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BusinessTransactionData getBusinessTransactionForId(int appId, int businessTxId) {
		return businessTransactions.get(new Pair<Integer, Integer>(appId, businessTxId));
	}

	/**
	 * Calculates an instance id from a pair of a name and a definition id.
	 *
	 * @param name
	 *            The name of the business transaction or application
	 * @param id
	 *            The id of the business transaction definition or application definition.
	 * @return The derived instance id for the application or business transaction.
	 */
	private int deriveInstanceId(String name, int id) {
		if (id == 0) {
			return 0;
		} else {
			final int prime = 31;
			int result = 1;
			result = (prime * result) + name.hashCode();
			result = (prime * result) + id;
			return result;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		if (log.isInfoEnabled()) {
			log.info("|-Business Context Management Service active...");
		}
	}
}
