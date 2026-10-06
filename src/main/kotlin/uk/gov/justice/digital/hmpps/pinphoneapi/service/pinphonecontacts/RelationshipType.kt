package uk.gov.justice.digital.hmpps.pinphoneapi.service.pinphonecontacts

enum class RelationshipType {
  SOCIAL,
  OFFICIAL,
  OTHER,
}

enum class BtRelationshipType(val id: Int, val description: String, val category: RelationshipType) {
  NOT_YET_ASSIGNED(0, "Not Yet Assigned", RelationshipType.OTHER),
  MOTHER(1, "Mother", RelationshipType.SOCIAL),
  FATHER(2, "Father", RelationshipType.SOCIAL),
  PARENTS(3, "Parents", RelationshipType.SOCIAL),
  SISTER(4, "Sister", RelationshipType.SOCIAL),
  BROTHER(5, "Brother", RelationshipType.SOCIAL),
  SON(6, "Son", RelationshipType.SOCIAL),
  DAUGHTER(7, "Daughter", RelationshipType.SOCIAL),
  WIFE(8, "Wife", RelationshipType.SOCIAL),
  HUSBAND(9, "Husband", RelationshipType.SOCIAL),
  AUNT(10, "Aunt", RelationshipType.SOCIAL),
  UNCLE(11, "Uncle", RelationshipType.SOCIAL),
  NEPHEW(12, "Nephew", RelationshipType.SOCIAL),
  NIECE(13, "Niece", RelationshipType.SOCIAL),
  COUSIN(14, "Cousin", RelationshipType.SOCIAL),
  GRANDFATHER(15, "Grandfather", RelationshipType.SOCIAL),
  GRANDMOTHER(16, "Grandmother", RelationshipType.SOCIAL),
  GRANDPARENTS(17, "Grandparents", RelationshipType.SOCIAL),
  GREAT_GRANDFATHER(18, "Great Grandfather", RelationshipType.SOCIAL),
  GREAT_GRANDMOTHER(19, "Great Grandmother", RelationshipType.SOCIAL),
  GRANDSON(20, "Grandson", RelationshipType.SOCIAL),
  GRANDDAUGHTER(21, "Granddaughter", RelationshipType.SOCIAL),
  GRANDCHILD(22, "Grandchild", RelationshipType.SOCIAL),
  PARTNER(23, "Partner", RelationshipType.SOCIAL),
  IN_LAWS(24, "In-laws", RelationshipType.SOCIAL),
  FRIEND(25, "Friend", RelationshipType.SOCIAL),
  COUNSELLOR(26, "Counsellor", RelationshipType.OFFICIAL),
  LEGAL_ADVISOR(27, "Legal Advisor", RelationshipType.OFFICIAL),
  SOLICITOR(28, "Solicitor", RelationshipType.OFFICIAL),
  PROBATION_OFFICER(29, "Probation Officer", RelationshipType.OFFICIAL),
  SOCIAL_WORKER(30, "Social Worker", RelationshipType.OFFICIAL),
  PRISON_VISITOR(31, "Prison Visitor", RelationshipType.OFFICIAL),
  TEACHER(32, "Teacher", RelationshipType.OFFICIAL),
  MINISTER(33, "Minister (all religions)", RelationshipType.OFFICIAL),
  EMBASSY_CONTACT(34, "Embassy Contact", RelationshipType.OFFICIAL),
  FINANCIAL_SERVICES(35, "Financial Services", RelationshipType.OFFICIAL),
  OTHER(36, "Other", RelationshipType.OTHER),
  ;

  companion object {
    private val byId = entries.associateBy { it.id }

    fun fromId(id: Int): BtRelationshipType = byId[id] ?: OTHER
  }
}
