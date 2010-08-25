package org.dcache.webadmin.controller.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import org.dcache.webadmin.controller.PoolAdminService;
import org.dcache.webadmin.controller.exceptions.PoolAdminServiceException;
import org.dcache.webadmin.controller.util.NamedCellUtil;
import org.dcache.webadmin.model.businessobjects.CellResponse;
import org.dcache.webadmin.model.businessobjects.NamedCell;
import org.dcache.webadmin.model.businessobjects.Pool;
import org.dcache.webadmin.model.dataaccess.DAOFactory;
import org.dcache.webadmin.model.dataaccess.DomainsDAO;
import org.dcache.webadmin.model.dataaccess.PoolGroupDAO;
import org.dcache.webadmin.model.dataaccess.PoolsDAO;
import org.dcache.webadmin.model.exceptions.DAOException;
import org.dcache.webadmin.view.beans.PoolAdminBean;
import org.dcache.webadmin.view.beans.PoolCommandBean;
import org.dcache.webadmin.view.util.SelectableWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jans
 */
public class StandardPoolAdminService implements PoolAdminService {

    private static final Logger _log = LoggerFactory.getLogger(
            StandardPoolAdminService.class);
    private DAOFactory _daoFactory;

    public StandardPoolAdminService(DAOFactory daoFactory) {
        _daoFactory = daoFactory;
    }

    @Override
    public List<PoolAdminBean> getPoolGroups() throws PoolAdminServiceException {
        try {
            Set<Pool> pools = getPoolsDAO().getPools();
            Set<String> poolGroups = getPoolGroupDAO().getPoolGroupNames();
            Map<String, NamedCell> namedCells = NamedCellUtil.createCellMap(
                    getDomainsDAO().getNamedCells());

            List<PoolAdminBean> adminBeans = new ArrayList<PoolAdminBean>();
            for (String currentPoolGroup : poolGroups) {
                PoolAdminBean newAdmin = createPoolAdminBean(
                        currentPoolGroup, pools, namedCells);
                adminBeans.add(newAdmin);
            }
            return adminBeans;
        } catch (DAOException e) {
            throw new PoolAdminServiceException(e);
        }

    }

    private DomainsDAO getDomainsDAO() {
        return _daoFactory.getDomainsDAO();
    }

    private PoolsDAO getPoolsDAO() {
        return _daoFactory.getPoolsDAO();
    }

    private PoolGroupDAO getPoolGroupDAO() {
        return _daoFactory.getPoolGroupDAO();
    }

    public void setDAOFactory(DAOFactory daoFactory) {
        _daoFactory = daoFactory;
    }

    private PoolAdminBean createPoolAdminBean(String currentPoolGroup,
            Set<Pool> pools, Map<String, NamedCell> namedCells) {
        PoolAdminBean newAdmin = new PoolAdminBean(currentPoolGroup);
        List<SelectableWrapper<PoolCommandBean>> groupPools =
                new ArrayList<SelectableWrapper<PoolCommandBean>>();
        for (Pool currentPool : pools) {
            if (currentPool.getPoolGroups().contains(currentPoolGroup)) {
                PoolCommandBean groupPool = new PoolCommandBean();
                groupPool.setName(currentPool.getName());
                NamedCell namedCell = namedCells.get(currentPool.getName());
                if (namedCell != null) {
                    groupPool.setDomain(namedCell.getDomainName());
                }
                groupPools.add(new SelectableWrapper<PoolCommandBean>(groupPool));
            }
        }
        newAdmin.setPools(groupPools);
        return newAdmin;
    }

    @Override
    public void sendCommand(
            List<SelectableWrapper<PoolCommandBean>> pools, String command)
            throws PoolAdminServiceException {
        Set<String> poolIds = getSelectedPools(pools);
        _log.debug("Sending commnd {} to following pools {}", command, poolIds);
        try {
            Set<CellResponse> responses = getDomainsDAO().sendCommand(poolIds, command);

            for (SelectableWrapper<PoolCommandBean> pool : pools) {
                clearResponse(pool);
                for (CellResponse response : responses) {
                    if (pool.getWrapped().getName().equals(response.getCellName())) {
                        pool.getWrapped().setResponse(response.getResponse());
                    }
                }
            }
        } catch (DAOException e) {
            throw new PoolAdminServiceException(e);
        }
    }

    private Set<String> getSelectedPools(List<SelectableWrapper<PoolCommandBean>> pools) {
        Set<String> poolIds = new HashSet<String>();
        for (SelectableWrapper<PoolCommandBean> pool : pools) {
            if (pool.isSelected()) {
                poolIds.add(pool.getWrapped().getName());
            }
        }
        return poolIds;
    }

    private void clearResponse(SelectableWrapper<PoolCommandBean> pool) {
        pool.getWrapped().setResponse("");
    }
}
