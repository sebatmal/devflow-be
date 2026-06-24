package com.sebatmal.devflow.api.project.service;

import com.sebatmal.devflow.api.project.dto.MemberResponse;
import com.sebatmal.devflow.api.project.dto.ProjectResponse;
import com.sebatmal.devflow.common.exception.DevflowException;
import com.sebatmal.devflow.db.member.repository.MemberRepository;
import com.sebatmal.devflow.db.project.entity.Project;
import com.sebatmal.devflow.db.project.repository.ProjectRepository;
import com.sebatmal.devflow.enums.message.FailMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;

    public ProjectResponse getProject(final Long projectId) {
        return ProjectResponse.from(findProject(projectId));
    }

    public List<MemberResponse> getMembers(final Long projectId) {
        final Project project = findProject(projectId);
        return memberRepository.findByOrganizationId(project.getOrganization().getId())
                .stream().map(MemberResponse::from).toList();
    }

    private Project findProject(final Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new DevflowException(FailMessage.NOT_FOUND_PROJECT));
    }
}
