package org.jmol.adapter.readers.cif;

import org.jmol.adapter.smarter.AtomSetCollectionReader;

public interface MagCifRdrInterface {

  void initialize(AtomSetCollectionReader r) throws Exception;

}

// see http://comcifs.github.io/magCIF.dic.html


//  Table of Contents
//  MAGNETIC
//  ATOM_SITE_MOMENT_FOURIER
//  _atom_site_moment_Fourier.atom_site_label
//  _atom_site_moment_Fourier.axis
//  _atom_site_moment_Fourier.id•
//  _atom_site_moment_Fourier.wave_vector_seq_id
//  ATOM_SITE_MOMENT_FOURIER_PARAM
//  _atom_site_moment_Fourier_param.cos
//  _atom_site_moment_Fourier_param.cos_symmform
//  _atom_site_moment_Fourier_param.id•
//  _atom_site_moment_Fourier_param.modulus
//  _atom_site_moment_Fourier_param.modulus_symmform
//  _atom_site_moment_Fourier_param.phase
//  _atom_site_moment_Fourier_param.phase_symmform
//  _atom_site_moment_Fourier_param.sin
//  _atom_site_moment_Fourier_param.sin_symmform
//  ATOM_SITE_MOMENT_SPECIAL_FUNC
//  _atom_site_moment_special_func.atom_site_label•
//  _atom_site_moment_special_func.sawtooth_ax
//  _atom_site_moment_special_func.sawtooth_ay
//  _atom_site_moment_special_func.sawtooth_az
//  _atom_site_moment_special_func.sawtooth_c
//  _atom_site_moment_special_func.sawtooth_w
//  ATOM_SITES_MOMENT_FOURIER
//  _atom_sites_moment_Fourier.axes_description
//  PARENT_PROPAGATION_VECTOR
//  _parent_propagation_vector.id•
//  _parent_propagation_vector.kxkykz
//  PARENT_SPACE_GROUP
//  _parent_space_group.child_transform_Pp_abc
//  _parent_space_group.IT_number
//  _parent_space_group.name_H-M_alt
//  _parent_space_group.reference_setting
//  _parent_space_group.transform_Pp_abc
//  SPACE_GROUP_MAGN
//  _space_group_magn.name_BNS
//  _space_group_magn.name_OG
//  _space_group_magn.number_BNS
//  _space_group_magn.OG_wavevector_kxkykz
//  _space_group_magn.point_group_name
//  _space_group_magn.point_group_number
//  _space_group_magn.ssg_name
//  _space_group_magn.ssg_number
//  _space_group_magn.transform_BNS_Pp
//  _space_group_magn.transform_BNS_Pp_abc
//  _space_group_magn.transform_OG_Pp
//  _space_group_magn.transform_OG_Pp_abc
//  SPACE_GROUP_MAGN_SSG_TRANSFORMS
//  _space_group_magn_ssg_transforms.description
//  _space_group_magn_ssg_transforms.id•
//  _space_group_magn_ssg_transforms.Pp_superspace
//  _space_group_magn_ssg_transforms.source
//  SPACE_GROUP_MAGN_TRANSFORMS
//  _space_group_magn_transforms.description
//  _space_group_magn_transforms.id•
//  _space_group_magn_transforms.Pp
//  _space_group_magn_transforms.Pp_abc
//  _space_group_magn_transforms.source
//  SPACE_GROUP_SYMOP_MAGN_CENTERING
//  _space_group_symop_magn_centering.description
//  _space_group_symop_magn_centering.id•
//  _space_group_symop_magn_centering.xyz
//  SPACE_GROUP_SYMOP_MAGN_OG_CENTERING
//  _space_group_symop_magn_OG_centering.description
//  _space_group_symop_magn_OG_centering.id•
//  _space_group_symop_magn_OG_centering.xyz
//  SPACE_GROUP_SYMOP_MAGN_OPERATION
//  _space_group_symop_magn_operation.description
//  _space_group_symop_magn_operation.id•
//  _space_group_symop_magn_operation.xyz
//  SPACE_GROUP_SYMOP_MAGN_SSG_CENTERING
//  _space_group_symop_magn_ssg_centering.algebraic
//  _space_group_symop_magn_ssg_centering.id•
//  SPACE_GROUP_SYMOP_MAGN_SSG_OPERATION
//  _space_group_symop_magn_ssg_operation.algebraic
//  _space_group_symop_magn_ssg_operation.id•
//  ATOM_SITE_MOMENT
//  _atom_site_moment.Cartn
//  _atom_site_moment.crystalaxis_x
//  _atom_site_moment.crystalaxis_y
//  _atom_site_moment.crystalaxis_z
//  _atom_site_moment.Cartn_z
//  _atom_site_moment.Cartn_x
//  _atom_site_moment.spherical_polar
//  _atom_site_moment.crystalaxis
//  _atom_site_moment.refinement_flags_magnetic
//  _atom_site_moment.Cartn_y
//  _atom_site_moment.label•
//  _atom_site_moment.spherical_azimuthal
//  _atom_site_moment.spherical_modulus
//  _atom_site_moment.modulation_flag
//  _atom_site_moment.symmform
//  ATOM_SITE_FOURIER_WAVE_VECTOR (Original category from: CIF_MS)
//  atom_site_Fourier_wave_vector.q3_coeff
//  atom_site_Fourier_wave_vector.q_coeff
//  atom_site_Fourier_wave_vector.q2_coeff
//  atom_site_Fourier_wave_vector.q1_coeff
//  ATOM_TYPE_SCAT (Original category from: CIF_CORE)
//  _atom_type_scat.neutron_magnetic_j0_D
//  _atom_type_scat.neutron_magnetic_j0_e
//  _atom_type_scat.neutron_magnetic_j2_a2
//  _atom_type_scat.neutron_magnetic_j2_A1
//  _atom_type_scat.neutron_magnetic_j2_D
//  _atom_type_scat.neutron_magnetic_j2_e
//  _atom_type_scat.neutron_magnetic_j4_D
//  _atom_type_scat.neutron_magnetic_j4_e
//  _atom_type_scat.neutron_magnetic_j4_c2
//  _atom_type_scat.neutron_magnetic_j6_C1
//  _atom_type_scat.neutron_magnetic_j6_c2
//  _atom_type_scat.neutron_magnetic_j4_C1
//  _atom_type_scat.neutron_magnetic_j6_a2
//  _atom_type_scat.neutron_magnetic_j6_A1
//  _atom_type_scat.neutron_magnetic_j4_A1
//  _atom_type_scat.neutron_magnetic_j4_a2
//  _atom_type_scat.neutron_magnetic_j0_A1
//  _atom_type_scat.neutron_magnetic_j6_D
//  _atom_type_scat.neutron_magnetic_j6_e
//  _atom_type_scat.neutron_magnetic_j0_B1
//  _atom_type_scat.neutron_magnetic_j2_C1
//  _atom_type_scat.neutron_magnetic_j0_b2
//  _atom_type_scat.neutron_magnetic_source
//  _atom_type_scat.neutron_magnetic_j2_c2
//  _atom_type_scat.neutron_magnetic_j0_a2
//  _atom_type_scat.neutron_magnetic_j4_B1
//  _atom_type_scat.neutron_magnetic_j4_b2
//  _atom_type_scat.neutron_magnetic_j6_b2
//  _atom_type_scat.neutron_magnetic_j6_B1
//  _atom_type_scat.neutron_magnetic_j2_b2
//  _atom_type_scat.neutron_magnetic_j2_B1
//  _atom_type_scat.neutron_magnetic_j0_c2
//  _atom_type_scat.neutron_magnetic_j0_C1
