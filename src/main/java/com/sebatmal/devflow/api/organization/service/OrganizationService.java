package com.sebatmal.devflow.api.organization.service;

import com.sebatmal.devflow.api.github.GithubApiClient;
import com.sebatmal.devflow.api.github.dto.GithubRepoResponse;
import com.sebatmal.devflow.api.github.dto.GithubUserResponse;
import com.sebatmal.devflow.api.organization.dto.ConnectResponse;
import com.sebatmal.devflow.api.project.dto.MemberResponse;
import com.sebatmal.devflow.api.project.dto.ProjectResponse;
import com.sebatmal.devflow.common.exception.DevflowException;
import com.sebatmal.devflow.db.member.entity.Member;
import com.sebatmal.devflow.db.member.repository.MemberRepository;
import com.sebatmal.devflow.db.organization.entity.Organization;
import com.sebatmal.devflow.db.organization.repository.OrganizationRepository;
import com.sebatmal.devflow.db.project.entity.Project;
import com.sebatmal.devflow.db.project.repository.ProjectRepository;
import com.sebatmal.devflow.db.user.entity.User;
import com.sebatmal.devflow.db.user.repository.UserRepository;
import com.sebatmal.devflow.enums.message.FailMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private static final String[] PALETTE = {
            "#7048E8", "#22C55E", "#FFB020", "#D6336C", "#3182F6", "#14B8A6", "#F97316", "#8B5CF6"
    };

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final GithubApiClient githubApiClient;

    /**
     * org 연결: GitHub에서 org 멤버·레포를 가져와 upsert.
     * 같은 org에 속한 사람들이 같은 데이터를 공유하게 되는 진입점.
     */
    @Transactional
    public ConnectResponse connect(final Long userId, final String orgLogin) {
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new DevflowException(FailMessage.NOT_FOUND_USER));
        final String token = requireGithubToken(user);

        final List<GithubUserResponse> githubMembers = githubApiClient.getOrgMembers(token, orgLogin);
        final List<GithubRepoResponse> githubRepos = githubApiClient.getOrgRepos(token, orgLogin);

        final Organization organization = organizationRepository.findByGithubLogin(orgLogin)
                .orElseGet(() -> organizationRepository.save(
                        Organization.builder().githubLogin(orgLogin).name(orgLogin).build()));

        upsertMembers(organization, githubMembers, user);
        upsertProjects(organization, githubRepos);

        final List<MemberResponse> members = memberRepository.findByOrganizationId(organization.getId())
                .stream().map(MemberResponse::from).toList();
        final List<ProjectResponse> projects = projectRepository.findByOrganizationId(organization.getId())
                .stream().map(ProjectResponse::from).toList();

        return new ConnectResponse(organization.getId(), organization.getGithubLogin(), organization.getName(), projects, members);
    }

    private void upsertMembers(final Organization organization, final List<GithubUserResponse> githubMembers, final User loginUser) {
        int index = 0;
        for (final GithubUserResponse gh : githubMembers) {
            final int colorIndex = index++;
            final Member member = memberRepository.findByOrganizationIdAndGithubLogin(organization.getId(), gh.login())
                    .map(existing -> {
                        existing.update(displayName(gh), gh.avatarUrl());
                        return existing;
                    })
                    .orElseGet(() -> memberRepository.save(Member.builder()
                            .organization(organization)
                            .githubLogin(gh.login())
                            .githubId(gh.id())
                            .name(displayName(gh))
                            .avatarUrl(gh.avatarUrl())
                            .color(PALETTE[colorIndex % PALETTE.length])
                            .build()));

            if (gh.id() != null && gh.id().equals(loginUser.getGithubId())) {
                member.linkUser(loginUser);
            }
        }
    }

    private void upsertProjects(final Organization organization, final List<GithubRepoResponse> githubRepos) {
        for (final GithubRepoResponse repo : githubRepos) {
            projectRepository.findByOrganizationIdAndGithubRepo(organization.getId(), repo.name())
                    .orElseGet(() -> projectRepository.save(Project.builder()
                            .organization(organization)
                            .githubRepo(repo.name())
                            .name(repo.name())
                            .build()));
        }
    }

    private String displayName(final GithubUserResponse gh) {
        return (gh.name() == null || gh.name().isBlank()) ? gh.login() : gh.name();
    }

    private String requireGithubToken(final User user) {
        if (user.getGithubAccessToken() == null || user.getGithubAccessToken().isBlank()) {
            throw new DevflowException(FailMessage.UNAUTHORIZED_NO_GITHUB_TOKEN);
        }
        return user.getGithubAccessToken();
    }
}
