package com.livo.works.Role.data

import com.livo.works.Search.data.PageInfo

data class PagedRoleRequests(
    val content: List<RoleRequestDto>,
    val page: PageInfo
)