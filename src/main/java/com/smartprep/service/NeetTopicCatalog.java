package com.smartprep.service;

import java.util.List;
import java.util.Map;

/**
 * Static catalog of NEET/JEE chapter and topic names used for search-box autocomplete.
 * Kept local so suggestions cost no YouTube API quota.
 */
final class NeetTopicCatalog {

    private NeetTopicCatalog() {}

    /** Topics keyed by subject name, matching the subject values the frontend sends. */
    static final Map<String, List<String>> TOPICS_BY_SUBJECT = Map.of(
            "Physics", List.of(
                    "Units and Measurements", "Motion in a Straight Line", "Motion in a Plane",
                    "Projectile Motion", "Newton's Laws of Motion", "Friction", "Circular Motion",
                    "Work, Energy and Power", "Conservation of Momentum", "Centre of Mass",
                    "Rotational Motion", "Moment of Inertia", "Gravitation", "Kepler's Laws",
                    "Mechanical Properties of Solids", "Mechanical Properties of Fluids",
                    "Bernoulli's Principle", "Viscosity", "Surface Tension", "Thermal Properties of Matter",
                    "Calorimetry", "Thermodynamics", "Kinetic Theory of Gases", "Oscillations",
                    "Simple Harmonic Motion", "Waves", "Doppler Effect", "Electric Charges and Fields",
                    "Coulomb's Law", "Gauss's Law", "Electrostatic Potential and Capacitance",
                    "Current Electricity", "Ohm's Law", "Kirchhoff's Laws", "Wheatstone Bridge",
                    "Moving Charges and Magnetism", "Magnetism and Matter", "Electromagnetic Induction",
                    "Faraday's Law", "Alternating Current", "LCR Circuit", "Electromagnetic Waves",
                    "Ray Optics", "Reflection of Light", "Refraction of Light", "Lens Formula",
                    "Optical Instruments", "Wave Optics", "Young's Double Slit Experiment", "Diffraction",
                    "Polarisation", "Dual Nature of Radiation and Matter", "Photoelectric Effect",
                    "Atoms", "Bohr Model", "Nuclei", "Radioactivity", "Semiconductor Electronics",
                    "Logic Gates", "P-N Junction Diode"),
            "Chemistry", List.of(
                    "Some Basic Concepts of Chemistry", "Mole Concept", "Stoichiometry",
                    "Structure of Atom", "Quantum Numbers", "Classification of Elements and Periodicity",
                    "Periodic Table", "Chemical Bonding and Molecular Structure", "VSEPR Theory",
                    "Hybridisation", "Molecular Orbital Theory", "States of Matter", "Gas Laws",
                    "Chemical Thermodynamics", "Enthalpy", "Entropy and Gibbs Energy", "Chemical Equilibrium",
                    "Ionic Equilibrium", "pH and Buffer Solutions", "Redox Reactions", "Hydrogen",
                    "s-Block Elements", "p-Block Elements", "d- and f-Block Elements", "Coordination Compounds",
                    "Solutions", "Colligative Properties", "Electrochemistry", "Nernst Equation",
                    "Chemical Kinetics", "Rate of Reaction", "Surface Chemistry", "Solid State",
                    "General Organic Chemistry", "Isomerism", "Hydrocarbons", "Alkanes", "Alkenes",
                    "Alkynes", "Aromatic Hydrocarbons", "Haloalkanes and Haloarenes",
                    "Alcohols, Phenols and Ethers", "Aldehydes, Ketones and Carboxylic Acids", "Amines",
                    "Biomolecules", "Polymers", "Chemistry in Everyday Life", "Environmental Chemistry",
                    "Qualitative Analysis", "Metallurgy"),
            "Biology", List.of(
                    "The Living World", "Biological Classification", "Plant Kingdom", "Animal Kingdom",
                    "Morphology of Flowering Plants", "Anatomy of Flowering Plants",
                    "Structural Organisation in Animals", "Cell: The Unit of Life", "Cell Theory",
                    "Cell Membrane", "Cell Organelles", "Mitochondria", "Chloroplast", "Nucleus",
                    "Biomolecules", "Enzymes", "Cell Cycle", "Cell Division", "Mitosis", "Meiosis",
                    "Transport in Plants", "Mineral Nutrition", "Photosynthesis in Higher Plants",
                    "Cellular Respiration", "Respiration in Plants", "Glycolysis", "Krebs Cycle",
                    "Plant Growth and Development", "Plant Hormones", "Digestion and Absorption",
                    "Breathing and Exchange of Gases", "Body Fluids and Circulation", "Human Heart",
                    "Excretory Products and their Elimination", "Locomotion and Movement",
                    "Neural Control and Coordination", "Chemical Coordination and Integration",
                    "Endocrine System", "Reproduction in Organisms", "Sexual Reproduction in Flowering Plants",
                    "Human Reproduction", "Reproductive Health", "Principles of Inheritance and Variation",
                    "Mendelian Genetics", "Sex Determination", "Genetic Disorders",
                    "Molecular Basis of Inheritance", "DNA Replication", "Transcription", "Translation",
                    "Genetic Code", "Lac Operon", "Human Genome Project", "DNA Fingerprinting", "Evolution",
                    "Origin of Life", "Hardy-Weinberg Principle", "Human Health and Disease", "Immunity",
                    "Microbes in Human Welfare", "Biotechnology: Principles and Processes",
                    "Recombinant DNA Technology", "PCR", "Biotechnology and its Applications",
                    "Organisms and Populations", "Ecosystem", "Biodiversity and Conservation",
                    "Environmental Issues"),
            "Maths", List.of(
                    "Sets", "Relations and Functions", "Trigonometric Functions", "Inverse Trigonometric Functions",
                    "Complex Numbers", "Quadratic Equations", "Linear Inequalities", "Permutations and Combinations",
                    "Binomial Theorem", "Sequences and Series", "Straight Lines", "Conic Sections", "Circles",
                    "Parabola", "Ellipse", "Hyperbola", "Three Dimensional Geometry", "Limits and Derivatives",
                    "Continuity and Differentiability", "Application of Derivatives", "Integrals",
                    "Definite Integrals", "Application of Integrals", "Differential Equations",
                    "Vector Algebra", "Matrices", "Determinants", "Linear Programming", "Probability",
                    "Statistics", "Mathematical Reasoning")
    );
}
