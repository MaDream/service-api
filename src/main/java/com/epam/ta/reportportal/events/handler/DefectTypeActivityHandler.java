package com.epam.ta.reportportal.events.handler;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.epam.ta.reportportal.database.dao.ActivityRepository;
import com.epam.ta.reportportal.database.dao.ProjectRepository;
import com.epam.ta.reportportal.database.entity.Project;
import com.epam.ta.reportportal.database.entity.item.Activity;
import com.epam.ta.reportportal.events.DefectTypeCreatedEvent;
import com.epam.ta.reportportal.events.DefectTypeDeletedEvent;
import com.epam.ta.reportportal.events.DefectTypeUpdatedEvent;
import com.epam.ta.reportportal.ws.converter.builders.ActivityBuilder;

/**
 * @author Andrei Varabyeu
 */
@Component
public class DefectTypeActivityHandler {

	private static final String UPDATE_DEFECT = "update_defect";
	private static final String DEFECT_TYPE = "defect_type";
	private static final String DELETE_DEFECT = "delete_defect";

	private final ActivityRepository activityRepository;
	private final Provider<ActivityBuilder> activityBuilder;

	@Autowired
	public DefectTypeActivityHandler(ProjectRepository projectSettingsRepository, ActivityRepository activityRepository,
			Provider<ActivityBuilder> activityBuilder) {
		this.activityRepository = activityRepository;
		this.activityBuilder = activityBuilder;
	}

	@EventListener
	public void onDefectTypeCreated(DefectTypeCreatedEvent event) {
		final Activity activity = activityBuilder.get().addLoggedObjectRef(event.getStatisticSubType().getLocator())
				.addProjectRef(event.getProject().toLowerCase()).addObjectType(DEFECT_TYPE).addActionType(UPDATE_DEFECT)
				.addUserRef(event.getUser()).build();
		activityRepository.save(activity);
	}

	@EventListener
	public void onDefectTypeUpdated(DefectTypeUpdatedEvent event) {
		List<Activity> activities = event.getRequest().getIds()
				.stream().map(r -> activityBuilder.get().addProjectRef(event.getProject()).addObjectType(DEFECT_TYPE)
						.addActionType(UPDATE_DEFECT).addLoggedObjectRef(r.getId()).addUserRef(event.getUpdatedBy()).build())
				.collect(Collectors.toList());
		activityRepository.save(activities);

	}

	@EventListener
	public void onDefectTypeDeleted(DefectTypeDeletedEvent event) {
		Project projectSettings = event.getBefore();
		projectSettings.getConfiguration().getSubTypes().values().stream().flatMap(Collection::stream)
				.filter(it -> it.getLocator().equalsIgnoreCase(event.getId())).findFirst().ifPresent(subType -> {
					Activity activity = activityBuilder.get().addProjectRef(projectSettings.getName()).addObjectType(DEFECT_TYPE)
							.addActionType(DELETE_DEFECT).addLoggedObjectRef(event.getId()).addUserRef(event.getUpdatedBy().toLowerCase())
							.addObjectName(subType.getLongName()).build();
					activityRepository.save(activity);
				});

	}
}
