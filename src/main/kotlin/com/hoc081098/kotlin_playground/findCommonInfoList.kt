package com.hoc081098.kotlin_playground

import com.hoc081098.kotlin_playground.BaseUser.Gender

// ------------------------------ MODELS ------------------------------

sealed interface BaseUser {
    val name: String
    val age: Int
    val favoriteIds: List<String>
    val gender: Gender
    val address: String

    enum class Gender {
        MALE,
        FEMALE,
    }
}

sealed interface Info {
    val value: String

    data class Name(override val value: String) : Info
    data class Age(override val value: String) : Info
    data class Ids(override val value: String) : Info
    data class Gender(override val value: String) : Info
    data class Address(override val value: String) : Info
}

class Me(
    override val name: String,
    override val age: Int,
    override val favoriteIds: List<String>,
    override val gender: Gender,
    override val address: String
) : BaseUser

class Partner(
    override val name: String,
    override val age: Int,
    override val favoriteIds: List<String>,
    override val gender: Gender,
    override val address: String
) : BaseUser

fun Me.findCommonInfoList(p: Partner): List<Info> = findCommonInfoListInternal(p)

// ------------------------------ LOGIC ------------------------------

/**
 * @return null if no common info, otherwise return common info as [String]
 */
private typealias FindCommonInfo<T> = (T, T) -> String?

private data class EqualInfo<T>(
    private val property: (BaseUser) -> T,
    private val findCommonInfo: FindCommonInfo<T>,
    private val stringToInfo: (String) -> Info,
) {
    fun BaseUser.findCommonInfo(other: BaseUser): Info? =
        findCommonInfo(property(this), property(other))
            ?.let(stringToInfo)
}

private object FindCommonInfos {
    @JvmField
    val string: FindCommonInfo<String> = { a, b -> if (a.isNotEmpty() && a == b) a else null }

    @JvmField
    val int: FindCommonInfo<Int> = { a, b -> if (a == b) a.toString() else null }

    @JvmField
    val stringList: FindCommonInfo<List<String>> = { lhs, rhs ->
        val rhsSet = rhs.toSet()
        lhs.filter { it in rhsSet }
            .ifEmpty { null }
            ?.joinToString(",")
    }

    @JvmField
    val gender: FindCommonInfo<Gender> = { lhs, rhs ->
        if (lhs == rhs) lhs.toString()
        else null
    }
}

private val equalInfos = listOf(
    EqualInfo(BaseUser::name, FindCommonInfos.string, Info::Name),
    EqualInfo(BaseUser::age, FindCommonInfos.int, Info::Age),
    EqualInfo(BaseUser::favoriteIds, FindCommonInfos.stringList, Info::Ids),
    EqualInfo(BaseUser::gender, FindCommonInfos.gender, Info::Gender),
    EqualInfo(BaseUser::address, FindCommonInfos.string, Info::Address),
)

private fun Me.findCommonInfoListInternal(p: Partner): List<Info> =
    equalInfos.mapNotNull { equalInfo ->
        with(equalInfo) { findCommonInfo(p) }
    }

fun main() {
    // [Name(value=name), Ids(value=2,3), Address(value=address)]
    println(
        Me(
            name = "name",
            age = 11,
            favoriteIds = listOf("1", "2", "3"),
            gender = Gender.MALE,
            address = "address"
        ).findCommonInfoList(
            Partner(
                name = "name",
                age = 10,
                favoriteIds = listOf("3", "2"),
                gender = Gender.FEMALE,
                address = "address"
            )
        )
    )
}