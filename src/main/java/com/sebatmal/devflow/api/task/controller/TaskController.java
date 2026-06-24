package com.sebatmal.devflow.api.task.controller;

import com.sebatmal.devflow.api.auth.resolver.AuthCredential;
import com.sebatmal.devflow.api.auth.resolver.Authentication;
import com.sebatmal.devflow.api.task.dto.CreateFeatureRequest;
import com.sebatmal.devflow.api.task.dto.CreateIssuesRequest;
import com.sebatmal.devflow.api.task.dto.CreateIssuesResponse;
import com.sebatmal.devflow.api.task.dto.MoveTaskRequest;
import com.sebatmal.devflow.api.task.dto.TaskResponse;
import com.sebatmal.devflow.api.task.dto.UpdateFeatureRequest;
import com.sebatmal.devflow.api.task.service.TaskService;
import com.sebatmal.devflow.common.response.APISuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}")
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/projects/{projectId}/tasks")
    public ResponseEntity<APISuccessResponse<List<TaskResponse>>> getTasks(
            @PathVariable("projectId") final Long projectId
    ) {
        return APISuccessResponse.of(HttpStatus.OK, taskService.getTasks(projectId));
    }

    @GetMapping("/projects/{projectId}/features")
    public ResponseEntity<APISuccessResponse<List<TaskResponse>>> getFeatures(
            @PathVariable("projectId") final Long projectId
    ) {
        return APISuccessResponse.of(HttpStatus.OK, taskService.getFeatures(projectId));
    }

    @GetMapping("/features/{featureId}")
    public ResponseEntity<APISuccessResponse<TaskResponse>> getFeature(
            @PathVariable("featureId") final Long featureId
    ) {
        return APISuccessResponse.of(HttpStatus.OK, taskService.getFeature(featureId));
    }

    @PostMapping("/projects/{projectId}/features")
    public ResponseEntity<APISuccessResponse<TaskResponse>> addFeature(
            @PathVariable("projectId") final Long projectId,
            @RequestBody @Valid final CreateFeatureRequest request
    ) {
        return APISuccessResponse.of(HttpStatus.CREATED, taskService.addFeature(projectId, request));
    }

    @PatchMapping("/features/{featureId}")
    public ResponseEntity<APISuccessResponse<TaskResponse>> updateFeature(
            @PathVariable("featureId") final Long featureId,
            @RequestBody @Valid final UpdateFeatureRequest request
    ) {
        return APISuccessResponse.of(HttpStatus.OK, taskService.updateFeature(featureId, request));
    }

    @DeleteMapping("/features/{featureId}")
    public ResponseEntity<APISuccessResponse<Void>> deleteFeature(
            @PathVariable("featureId") final Long featureId
    ) {
        taskService.deleteFeature(featureId);
        return APISuccessResponse.of(HttpStatus.OK, null);
    }

    @PatchMapping("/tasks/{taskId}")
    public ResponseEntity<APISuccessResponse<TaskResponse>> moveTask(
            @PathVariable("taskId") final Long taskId,
            @RequestBody @Valid final MoveTaskRequest request
    ) {
        return APISuccessResponse.of(HttpStatus.OK, taskService.moveTask(taskId, request.week()));
    }

    @PostMapping("/features/{featureId}/issues")
    public ResponseEntity<APISuccessResponse<CreateIssuesResponse>> createIssues(
            @Authentication final AuthCredential authCredential,
            @PathVariable("featureId") final Long featureId,
            @RequestBody @Valid final CreateIssuesRequest request
    ) {
        return APISuccessResponse.of(HttpStatus.CREATED,
                taskService.createIssues(authCredential.userId(), featureId, request));
    }
}
