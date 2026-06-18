const links = Array.from(document.querySelectorAll('.nav a'));
const sections = links
    .map(link => document.querySelector(link.getAttribute('href')))
    .filter(Boolean);

const updateActiveLink = () => {
    let currentId = sections[0]?.id ?? '';
    const offset = window.scrollY + 140;

    for (const section of sections) {
        if (section.offsetTop <= offset) {
            currentId = section.id;
        }
    }

    links.forEach(link => {
        const target = link.getAttribute('href')?.replace('#', '');
        link.classList.toggle('active', target === currentId);
    });
};

document.addEventListener('scroll', updateActiveLink, { passive: true });
window.addEventListener('load', updateActiveLink);
