/*
 * Copyright 2016 EPAM Systems
 * 
 * 
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/service-api
 * 
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * This file is part of Report Portal.
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.ta.reportportal.ws.controller.impl;

import com.epam.ta.reportportal.core.jasper.IGetJasperReportHandler;
import com.epam.ta.reportportal.core.jasper.ReportFormat;
import com.epam.ta.reportportal.core.launch.*;
import com.epam.ta.reportportal.database.entity.Launch;
import com.epam.ta.reportportal.database.search.Condition;
import com.epam.ta.reportportal.database.search.Filter;
import com.epam.ta.reportportal.ws.controller.ILaunchController;
import com.epam.ta.reportportal.ws.model.BulkRQ;
import com.epam.ta.reportportal.ws.model.EntryCreatedRS;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.launch.*;
import com.epam.ta.reportportal.ws.model.widget.ChartObject;
import com.epam.ta.reportportal.ws.resolver.FilterCriteriaResolver;
import com.epam.ta.reportportal.ws.resolver.FilterFor;
import com.epam.ta.reportportal.ws.resolver.SortFor;
import com.google.common.net.HttpHeaders;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import net.sf.jasperreports.engine.JasperPrint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.epam.ta.reportportal.auth.permissions.Permissions.ASSIGNED_TO_PROJECT;
import static com.epam.ta.reportportal.auth.permissions.Permissions.PROJECT_MEMBER;
import static com.epam.ta.reportportal.commons.EntityUtils.normalizeProjectName;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 * Controller implementation for
 * {@link com.epam.ta.reportportal.database.entity.Launch} entity
 * <p>
 * Note: please use EntityUtils for forced lower case for user names and project
 * names
 * </p>
 *
 * @author Andrei Varabyeu
 * @author Andrei Kliashchonak
 * @author Andrei_Ramanchuk
 */
@Controller
@RequestMapping("/{projectName}/launch")
@PreAuthorize(ASSIGNED_TO_PROJECT)
public class LaunchController implements ILaunchController {

	@Autowired
	private IStartLaunchHandler createLaunchMessageHandler;

	@Autowired
	private IFinishLaunchHandler finishLaunchMessageHandler;

	@Autowired
	private IDeleteLaunchHandler deleteLaunchMessageHandler;

	@Autowired
	private IGetLaunchHandler getLaunchMessageHandler;

	@Autowired
	private IUpdateLaunchHandler updateLaunchHandler;

	@Autowired
	private IGetJasperReportHandler getJasperHandler;

	@Autowired
	private IMergeLaunchHandler mergeLaunchesHandler;

	@Override
	@PostMapping
	@ResponseBody
	@ResponseStatus(CREATED)
	@ApiOperation("Starts launch for specified project")
	public EntryCreatedRS startLaunch(
			@ApiParam(value = "Name of project launch starts under", required = true) @PathVariable String projectName,
			@ApiParam(value = "Start launch request body", required = true) @RequestBody @Validated StartLaunchRQ startLaunchRQ,
			Principal principal) {
		return createLaunchMessageHandler.startLaunch(principal.getName(), normalizeProjectName(projectName), startLaunchRQ);
	}

	@Override
	@RequestMapping(value = "/{launchId}/finish", method = PUT)
	@ResponseBody
	@ResponseStatus(OK)
	@ApiOperation("Finish launch for specified project")
	public OperationCompletionRS finishLaunch(@PathVariable String projectName, @PathVariable String launchId,
			@RequestBody @Validated FinishExecutionRQ finishLaunchRQ, Principal principal, HttpServletRequest request) {
		return finishLaunchMessageHandler.finishLaunch(launchId, finishLaunchRQ, normalizeProjectName(projectName), principal.getName());
	}

	@Override
	@PutMapping("/{launchId}/stop")
	@ResponseBody
	@ResponseStatus(OK)
	@ApiOperation("Force finish launch for specified project")
	public OperationCompletionRS forceFinishLaunch(@PathVariable String projectName, @PathVariable String launchId,
			@RequestBody @Validated FinishExecutionRQ finishExecutionRQ, Principal principal) {
		return finishLaunchMessageHandler.stopLaunch(launchId, finishExecutionRQ, normalizeProjectName(projectName), principal.getName());
	}

	@Override
	@PutMapping("/stop")
	@ResponseBody
	@ResponseStatus(OK)
	@ApiOperation("Force finish launch")
	public List<OperationCompletionRS> bulkForceFinish(@PathVariable String projectName,
			@RequestBody @Validated BulkRQ<FinishExecutionRQ> rq, Principal principal) {
		return finishLaunchMessageHandler.stopLaunch(rq, normalizeProjectName(projectName), principal.getName());
	}

	@Override
	@PutMapping("/{launchId}/update")
	@ResponseBody
	@ResponseStatus(OK)
	@PreAuthorize(ASSIGNED_TO_PROJECT)
	@ApiOperation("Updates launch for specified project")
	public OperationCompletionRS updateLaunch(@PathVariable String projectName, @PathVariable String launchId,
			@RequestBody @Validated UpdateLaunchRQ updateLaunchRQ, Principal principal) {
		return updateLaunchHandler.updateLaunch(launchId, normalizeProjectName(projectName), principal.getName(), updateLaunchRQ);
	}

	@Override
	@ResponseBody
	@PutMapping("/update")
	@PreAuthorize(ASSIGNED_TO_PROJECT)
	@ResponseStatus(OK)
    @ApiOperation("Updates launches for specified project")
	public List<OperationCompletionRS> updateLaunches(@PathVariable String projectName, @RequestBody @Validated BulkRQ<UpdateLaunchRQ> rq,
			Principal principal) {
		return updateLaunchHandler.updateLaunch(rq, normalizeProjectName(projectName), principal.getName());
	}

	@Override
	@DeleteMapping("/{launchId}")
	@ResponseBody
	@ResponseStatus(OK)
	@ApiOperation("Delete specified launch by ID")
	public OperationCompletionRS deleteLaunch(@PathVariable String projectName, @PathVariable String launchId, Principal principal) {
		return deleteLaunchMessageHandler.deleteLaunch(launchId, normalizeProjectName(projectName), principal.getName());
	}

	@Override
	@GetMapping("/{launchId}")
	@ResponseBody
	@ResponseStatus(OK)
	@ApiOperation("Get specified launch")
	public LaunchResource getLaunch(@PathVariable String projectName, @PathVariable String launchId, Principal principal) {
		return getLaunchMessageHandler.getLaunch(launchId, principal.getName(), normalizeProjectName(projectName));
	}

	@Override
	@GetMapping
	@ResponseBody
	@ResponseStatus(OK)
	@ApiOperation("Get list of project launches by filter")
	public Iterable<LaunchResource> getProjectLaunches(@PathVariable String projectName, @FilterFor(Launch.class) Filter filter,
			@SortFor(Launch.class) Pageable pageable, Principal principal) {
		return getLaunchMessageHandler.getProjectLaunches(normalizeProjectName(projectName), filter, pageable, principal.getName());
	}

	@Override
	@RequestMapping(value = "/mode", method = GET)
	@ResponseBody
	@ResponseStatus(OK)
	@PreAuthorize(PROJECT_MEMBER)
	@ApiOperation("Get launches of specified project from DEBUG mode")
	public Iterable<LaunchResource> getDebugLaunches(@PathVariable String projectName, @FilterFor(Launch.class) Filter filter,
			@SortFor(Launch.class) Pageable pageable, Principal principal) {
		return getLaunchMessageHandler.getDebugLaunches(normalizeProjectName(projectName), principal.getName(), filter, pageable);
	}

	@Override
	@RequestMapping(value = "/tags", method = GET)
	@ResponseBody
	@ResponseStatus(OK)
	@ApiOperation("Get all unique tags of project launches")
	public List<String> getAllTags(@PathVariable String projectName,
			@RequestParam(value = FilterCriteriaResolver.DEFAULT_FILTER_PREFIX + Condition.CNT + Launch.TAGS) String value,
			Principal principal) {
		return getLaunchMessageHandler.getTags(normalizeProjectName(projectName), value);
	}

	@Override
	@RequestMapping(value = "/owners", method = GET)
	@ResponseBody
	@ResponseStatus(OK)
	@ApiOperation("Get all unique owners of project launches")
	public List<String> getAllOwners(@PathVariable String projectName,
			 @RequestParam(value = FilterCriteriaResolver.DEFAULT_FILTER_PREFIX + Condition.CNT + Launch.USER) String value,
			 @RequestParam(value = "mode", required = false, defaultValue = "DEFAULT") String mode, Principal principal) {
		return getLaunchMessageHandler.getOwners(normalizeProjectName(projectName), value, "userRef", mode);
	}

	@Override
	@RequestMapping(value = "/names", method = GET)
	@ResponseBody
	@ResponseStatus(OK)
	@ApiOperation("Get launch names of project")
	public List<String> getAllLaunchNames(@PathVariable String projectName,
			@RequestParam(value = FilterCriteriaResolver.DEFAULT_FILTER_PREFIX + Condition.CNT + Launch.NAME) String value,
			Principal principal) {
		return getLaunchMessageHandler.getLaunchNames(normalizeProjectName(projectName), value);
	}

	@Override
	@RequestMapping(value = "/compare", method = GET)
	@ResponseBody
	@ResponseStatus(OK)
	@ApiOperation("Compare launches")
	public Map<String, List<ChartObject>> compareLaunches(@PathVariable String projectName, @RequestParam(value = "ids") String[] ids,
			Principal principal) {
		return getLaunchMessageHandler.getLaunchesComparisonInfo(normalizeProjectName(projectName), ids);
	}

	@Override
	@PostMapping("/merge")
	@ResponseBody
	@ResponseStatus(OK)
	// Owner and project LEAD+ permissions validated inside cause no information
	// on top resource level
	@ApiOperation("Merge set of specified launches in common one")
	public LaunchResource mergeLaunches(
			@ApiParam(value = "Name of project contains merging launches under", required = true) @PathVariable String projectName,
			@ApiParam(value = "Merge launches request body", required = true) @RequestBody @Validated MergeLaunchesRQ mergeLaunchesRQ,
			Principal principal) {
		return mergeLaunchesHandler.mergeLaunches(normalizeProjectName(projectName), principal.getName(), mergeLaunchesRQ);
	}

	@Override
	@PostMapping("/{launchId}/merge")
	@ResponseBody
	@ResponseStatus(OK)
	@ApiIgnore
	public OperationCompletionRS deepMergeLaunches(
			@ApiParam(value = "Name of project contains merging launches under", required = true) @PathVariable String projectName,
			@PathVariable String launchId,
			@ApiParam(value = "Merge launches request body", required = true) @RequestBody @Validated DeepMergeLaunchesRQ mergeLaunchesRQ,
			Principal principal) {
		return mergeLaunchesHandler.deepMergeLaunches(normalizeProjectName(projectName), launchId, principal.getName(), mergeLaunchesRQ);
	}

	@Override
	@RequestMapping(value = "/{launchId}/analyze/{strategy}", method = POST)
	@ResponseBody
	@ResponseStatus(OK)
	@ApiOperation("Start launch auto-analyzer on demand")
	public OperationCompletionRS startLaunchAnalyzer(@PathVariable String projectName, @PathVariable String launchId,
			@ApiParam(allowableValues = "single, history") @PathVariable String strategy, Principal principal) throws InterruptedException, ExecutionException {
		return updateLaunchHandler.startLaunchAnalyzer(normalizeProjectName(projectName), launchId, strategy);
	}

	@Override
	@RequestMapping(value = "/status", method = GET)
	@ResponseBody
	@ResponseStatus(OK)
	@ApiIgnore
	public Map<String, String> getStatuses(@PathVariable String projectName, @RequestParam(value = "ids") String[] ids,
			Principal principal) {
		return getLaunchMessageHandler.getStatuses(normalizeProjectName(projectName), ids);
	}

	@Override
	@RequestMapping(value = "/{launchId}/report", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(OK)
	@ApiOperation(value = "Export specified launch", notes = "Only following formats are supported: pdf (by default), xml, xls, html.")
	public void getLaunchReport(@PathVariable String projectName, @PathVariable String launchId,
			@ApiParam(allowableValues = "pdf, xml, xls, html") @RequestParam(value = "view", required = false, defaultValue = "pdf") String view,
            Principal principal, HttpServletResponse response) throws IOException {

		JasperPrint jasperPrint = getJasperHandler.getLaunchDetails(launchId, principal.getName());

		ReportFormat format = getJasperHandler.getReportFormat(view);
		response.setContentType(format.getContentType());
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
				String.format("attachment; filename=RP_%s_Report.%s", format.name(), format.getValue()));

		getJasperHandler.writeReport(format, response.getOutputStream(), jasperPrint);

	}

	@Override
	@DeleteMapping
	@ResponseBody
	@ResponseStatus(OK)
	@ApiOperation("Delete specified launches by ids")
	public OperationCompletionRS deleteLaunches(@PathVariable String projectName, @RequestParam(value = "ids") String[] ids,
			Principal principal) {
		return deleteLaunchMessageHandler.deleteLaunches(ids, projectName, principal.getName());
	}
}