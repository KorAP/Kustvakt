package de.ids_mannheim.korap.service;

import de.ids_mannheim.korap.constant.QueryType;
import de.ids_mannheim.korap.entity.QueryDO;

public class DummyQueryServiceImpl implements QueryServiceInterface {

	@Override
	public QueryDO searchQueryByName(String username, String vcName,
			String vcOwner, QueryType queryType) {
		return null;
	}

}
