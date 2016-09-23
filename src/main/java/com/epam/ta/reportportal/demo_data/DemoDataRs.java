package com.epam.ta.reportportal.demo_data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
class DemoDataRs {

	private List<String> dashboards;
	private List<String> launches;

	List<String> getDashboards() {
		return dashboards;
	}

	void setDashboards(List<String> dashboards) {
		this.dashboards = dashboards;
	}

	List<String> getLaunches() {
		return launches;
	}

	void setLaunches(List<String> launches) {
		this.launches = launches;
	}
}
